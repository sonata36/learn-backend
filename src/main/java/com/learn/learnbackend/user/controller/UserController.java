package com.learn.learnbackend.user.controller;

import com.learn.learnbackend.auth.CurrentUser;
import com.learn.learnbackend.common.ApiResponse;
import com.learn.learnbackend.user.dto.ChangePasswordRequest;
import com.learn.learnbackend.user.dto.UpdateProfileRequest;
import com.learn.learnbackend.user.dto.UserResponse;
import com.learn.learnbackend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户自身资料接口：只能操作自己，id 取自 CurrentUser，绝不信任请求体传的 id。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(CurrentUser currentUser) {
        return ApiResponse.ok(userService.getCurrentUser(currentUser.id()));
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateProfile(CurrentUser currentUser,
                                                   @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(currentUser.id(), request));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(CurrentUser currentUser,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUser.id(), request);
        return ApiResponse.ok();
    }
}
