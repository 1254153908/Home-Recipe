package org.huhu.recipe.auth.service;

import org.huhu.recipe.auth.dto.LoginRequest;
import org.huhu.recipe.auth.dto.LoginResponse;
import org.huhu.recipe.auth.dto.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest req);
    LoginResponse login(LoginRequest req);
}
