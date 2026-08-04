package org.huhu.recipe.auth.controller;

import org.huhu.recipe.auth.config.UserContext;
import org.huhu.recipe.auth.dto.*;
import org.huhu.recipe.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest req) {
        authService.register(req);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "注册成功");
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req) {
        LoginResponse resp = authService.login(req);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", resp);
        return result;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        Long userId = UserContext.getUserId();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("userId", userId);
        return result;
    }
}
