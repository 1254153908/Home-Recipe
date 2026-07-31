package org.huhu.recipe.common.controller;

import org.huhu.recipe.common.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String url = fileUploadService.upload(file);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return ResponseEntity.ok(result);
    }

    /**
     * 通过网络图片 URL 下载图片并上传到 MinIO / 本地存储
     * 请求体: {"url": "https://example.com/image.jpg"}
     */
    @PostMapping("/upload/url")
    public ResponseEntity<Map<String, String>> uploadFromUrl(@RequestBody Map<String, String> body) {
        String imageUrl = body.get("url");
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("url参数不能为空");
        }
        String resultUrl = fileUploadService.uploadFromUrl(imageUrl);
        Map<String, String> result = new HashMap<>();
        result.put("url", resultUrl);
        return ResponseEntity.ok(result);
    }
}
