package com.learn.learnbackend.auth;

import com.learn.learnbackend.common.ApiResponse;
import com.learn.learnbackend.user.dto.LoginRequest;
import com.learn.learnbackend.user.dto.RegisterRequest;
import com.learn.learnbackend.user.dto.UserResponse;
import com.learn.learnbackend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 * 阶段 2：注册 + 登录（仅校验账密并返回用户信息）。
 * 阶段 3：登录在此签发并下发 Cookie / Session / Token 凭据，退出在此清理凭据。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(userService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }
}
