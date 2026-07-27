package org.huhu.recipe.Service.impl;

import org.huhu.recipe.Service.AiRecognitionService;
import org.huhu.recipe.dto.AiRecognizeRequest;
import org.huhu.recipe.dto.RecipeDraft;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AiRecognitionServiceImpl implements AiRecognitionService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ai.mock-service.url}")
    private String mockServiceUrl;

    @Override
    public RecipeDraft recognize(AiRecognizeRequest request) {
        // 调用 Flask 假数据服务，返回可自动填充的菜谱草稿
        return restTemplate.postForObject(mockServiceUrl, request, RecipeDraft.class);
    }
}
