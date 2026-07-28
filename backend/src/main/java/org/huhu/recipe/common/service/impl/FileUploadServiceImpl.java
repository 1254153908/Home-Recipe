package org.huhu.recipe.common.service.impl;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.huhu.recipe.common.config.MinioProperties;
import org.huhu.recipe.common.service.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
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
                    return minioProps.getEndpoint() + "/" + minioProps.getBucket() + "/" + filename;
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
}
