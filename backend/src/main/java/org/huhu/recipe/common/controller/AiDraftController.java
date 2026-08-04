package org.huhu.recipe.common.controller;

import org.huhu.recipe.auth.config.UserContext;
import org.huhu.recipe.common.dto.RecipeDraft;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class AiDraftController {

    private static final String KEY_PREFIX = "user:";
    private static final String KEY_SUFFIX = ":aiDraft";
    private static final long TTL_MINUTES = 5;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/ai-draft")
    public Map<String, Object> save(@RequestBody RecipeDraft draft) {
        Long userId = UserContext.getUserId();
        String key = KEY_PREFIX + userId + KEY_SUFFIX;
        redisTemplate.opsForValue().set(key, draft, TTL_MINUTES, TimeUnit.MINUTES);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    @GetMapping("/ai-draft")
    public Map<String, Object> get() {
        Long userId = UserContext.getUserId();
        String key = KEY_PREFIX + userId + KEY_SUFFIX;
        RecipeDraft draft = (RecipeDraft) redisTemplate.opsForValue().get(key);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", draft);
        return result;
    }

    @DeleteMapping("/ai-draft")
    public Map<String, Object> delete() {
        Long userId = UserContext.getUserId();
        String key = KEY_PREFIX + userId + KEY_SUFFIX;
        redisTemplate.delete(key);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }
}
