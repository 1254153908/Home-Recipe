package org.huhu.recipe.common.service.impl;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.huhu.recipe.common.config.MinioProperties;
import org.huhu.recipe.common.service.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadServiceImpl.class);

    @Value("${upload.local-dir:uploads}")
    private String localDir;

    @Autowired
    private MinioProperties minioProps;

    @Autowired(required = false)
    private MinioClient minioClient;

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("仅支持图片文件上传，当前类型: " + contentType);
        }

        try {
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + ext;

            // 使用绝对路径，避免 JVM 工作目录不一致导致路径错误
            Path localPath = Paths.get(localDir).toAbsolutePath().normalize().resolve(filename);
            log.info("开始上传文件: originalName={}, targetPath={}, size={} bytes",
                    originalName, localPath, file.getSize());

            // 确保目录存在
            Files.createDirectories(localPath.getParent());

            // 保存到本地
            file.transferTo(localPath.toFile());
            log.info("文件已保存到本地: {}", localPath);

            // 如果 MinIO 已启用，尝试转存到 MinIO；失败则降级回本地 URL
            if (minioProps.isEnabled() && minioClient != null) {
                try (InputStream is = Files.newInputStream(localPath)) {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(minioProps.getBucket())
                            .object(filename)
                            .stream(is, Files.size(localPath), -1)
                            .contentType(file.getContentType())
                            .build());
                    log.info("文件已转存到 MinIO: bucket={}, object={}", minioProps.getBucket(), filename);
                    Files.deleteIfExists(localPath);
                    return "/images/" + minioProps.getBucket() + "/" + filename;
                } catch (Exception minioEx) {
                    log.warn("MinIO 上传失败，降级为本地存储: {}", minioEx.getMessage());
                }
            }

            // MinIO 未启用或上传失败，返回本地 URL
            return "/uploads/" + filename;
        } catch (IllegalArgumentException e) {
            throw e; // 参数校验异常直接抛出
        } catch (Exception e) {
            log.error("文件上传失败: originalName={}, error={}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("图片URL不能为空");
        }

        try {
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            // 校验文件类型（仅当明确返回非图片类型时才拒绝）
            String contentType = connection.getContentType();
            if (contentType != null && !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("仅支持图片文件上传，当前类型: " + contentType);
            }
            if (contentType == null) {
                contentType = "image/jpeg"; // CDN 可能不返回 Content-Type，默认使用 image/*
            }

            String ext = "";
            // 去掉查询参数后从URL路径提取扩展名
            String urlPath = imageUrl.contains("?") ? imageUrl.substring(0, imageUrl.indexOf("?")) : imageUrl;
            if (urlPath.contains(".")) {
                String candidate = urlPath.substring(urlPath.lastIndexOf("."));
                // 确保不是域名中的点
                if (!candidate.contains("/")) {
                    ext = candidate;
                }
            }
            String filename = UUID.randomUUID().toString() + ext;

            // 使用绝对路径，避免 JVM 工作目录不一致导致路径错误
            Path localPath = Paths.get(localDir).toAbsolutePath().normalize().resolve(filename);
            log.info("开始从URL上传图片: url={}, targetPath={}",
                    imageUrl, localPath);

            // 确保目录存在
            Files.createDirectories(localPath.getParent());

            // 下载到本地
            try (InputStream is = connection.getInputStream()) {
                Files.copy(is, localPath);
            }
            log.info("图片已下载到本地: {}", localPath);

            // 如果 MinIO 已启用，尝试转存到 MinIO；失败则降级回本地 URL
            if (minioProps.isEnabled() && minioClient != null) {
                try (InputStream is = Files.newInputStream(localPath)) {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(minioProps.getBucket())
                            .object(filename)
                            .stream(is, Files.size(localPath), -1)
                            .contentType(contentType)
                            .build());
                    log.info("文件已转存到 MinIO: bucket={}, object={}", minioProps.getBucket(), filename);
                    Files.deleteIfExists(localPath);
                    return "/images/" + minioProps.getBucket() + "/" + filename;
                } catch (Exception minioEx) {
                    log.warn("MinIO 上传失败，降级为本地存储: {}", minioEx.getMessage());
                }
            }

            // MinIO 未启用或上传失败，返回本地 URL
            return "/uploads/" + filename;
        } catch (IllegalArgumentException e) {
            throw e; // 参数校验异常直接抛出
        } catch (Exception e) {
            log.error("从URL上传图片失败: url={}, error={}", imageUrl, e.getMessage(), e);
            throw new RuntimeException("从URL上传图片失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String url) {
        if (url == null || url.isBlank()) {
            return;
        }

        try {
            // MinIO URL
            if (minioProps.isEnabled() && minioClient != null && url.startsWith(minioProps.getEndpoint())) {
                String prefix = minioProps.getEndpoint() + "/" + minioProps.getBucket() + "/";
                if (url.startsWith(prefix)) {
                    String objectName = url.substring(prefix.length());
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(minioProps.getBucket())
                            .object(objectName)
                            .build());
                    log.info("已从 MinIO 删除: bucket={}, object={}", minioProps.getBucket(), objectName);
                    return;
                }
            }

            // 本地 URL
            if (url.startsWith("/uploads/")) {
                String filename = url.substring("/uploads/".length());
                Path localPath = Paths.get(localDir).toAbsolutePath().normalize().resolve(filename);
                Files.deleteIfExists(localPath);
                log.info("已从本地删除: {}", localPath);
            }
        } catch (Exception e) {
            log.warn("删除图片失败，忽略: url={}, error={}", url, e.getMessage());
        }
    }
}
