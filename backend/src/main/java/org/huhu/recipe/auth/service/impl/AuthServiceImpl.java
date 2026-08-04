package org.huhu.recipe.auth.service.impl;

import org.huhu.recipe.auth.config.JwtUtil;
import org.huhu.recipe.auth.dto.*;
import org.huhu.recipe.auth.entity.User;
import org.huhu.recipe.auth.mapper.UserMapper;
import org.huhu.recipe.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private static final int MIN_PASSWORD_LENGTH = 6;

    @Override
    public void register(RegisterRequest req) {
        if (req.getUsername() == null || req.getUsername().trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (req.getPassword() == null || req.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new RuntimeException("密码至少需要" + MIN_PASSWORD_LENGTH + "位");
        }

        User exist = userMapper.selectByUsername(req.getUsername());
        if (exist != null) {
            throw new RuntimeException("该用户名已存在");
        }

        String hash = encoder.encode(req.getPassword());

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPasswordHash(hash);
        user.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        userMapper.insert(user);
    }

    @Override
    public LoginResponse login(LoginRequest req) {
        User user = userMapper.selectByUsername(req.getUsername());
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .build();
    }
}
