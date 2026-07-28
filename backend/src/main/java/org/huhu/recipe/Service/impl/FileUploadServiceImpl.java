package org.huhu.recipe.Service.impl;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.huhu.recipe.Service.FileUploadService;
import org.huhu.recipe.config.MinioProperties;
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

    @Value("${upload.local-dir:uploads}")
    private String localDir;

    @Autowired
    private MinioProperties minioProps;

    @Autowired(required = false)
    private MinioClient minioClient;

    @Override
    public String upload(MultipartFile file) {
        try {
            String ext = "";
            String originalName = file.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + ext;

            // 1. 先保存到本地
            Path localPath = Paths.get(localDir, filename);
            Files.createDirectories(localPath.getParent());
            file.transferTo(localPath.toFile());

            // 2. 如果 MinIO 已启用，转存到 MinIO
            if (minioProps.isEnabled() && minioClient != null) {
                try (InputStream is = Files.newInputStream(localPath)) {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(minioProps.getBucket())
                            .object(filename)
                            .stream(is, Files.size(localPath), -1)
                            .contentType(file.getContentType())
                            .build());
                }
                return minioProps.getEndpoint() + "/" + minioProps.getBucket() + "/" + filename;
            }

            // 3. MinIO 未启用，返回本地 URL
            return "/uploads/" + filename;
        } catch (Exception e) {
            throw new RuntimeException("File upload failed", e);
        }
    }
}
