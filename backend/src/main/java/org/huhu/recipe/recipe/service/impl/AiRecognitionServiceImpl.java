package org.huhu.recipe.recipe.service.impl;

import org.huhu.recipe.common.dto.AiRecognizeRequest;
import org.huhu.recipe.common.dto.RecipeDraft;
import org.huhu.recipe.common.service.FileUploadService;
import org.huhu.recipe.recipe.service.AiRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AiRecognitionServiceImpl implements AiRecognitionService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ai.mock-service.url}")
    private String mockServiceUrl;

    @Autowired
    private FileUploadService fileUploadService;

    private void uploadImage(RecipeDraft draft) {
        // 上传图片到 MinIO
        // 上传封面图片
        String coverImageUrl = fileUploadService.uploadFromUrl(draft.getImageUrl());
        draft.setImageUrl(coverImageUrl);
        //上传步骤图片
        for (int i = 0; i < draft.getSteps().size(); i++) {
            String stepImageUrl = fileUploadService.uploadFromUrl(draft.getSteps().get(i).getImageUrl());
            draft.getSteps().get(i).setImageUrl(stepImageUrl);
        }
    }

    @Override
    public RecipeDraft recognize(AiRecognizeRequest request) {

        RecipeDraft draft = restTemplate.postForObject(mockServiceUrl, request, RecipeDraft.class);
        // 调用 Flask 假数据服务，返回可自动填充的菜谱草稿
        System.out.println(draft);
        uploadImage(draft);
        return draft;
    }
}
