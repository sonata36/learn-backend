package com.learn.learnbackend.user.service;

import com.learn.learnbackend.user.dto.ChangePasswordRequest;
import com.learn.learnbackend.user.dto.LoginRequest;
import com.learn.learnbackend.user.dto.RegisterRequest;
import com.learn.learnbackend.user.dto.UpdateProfileRequest;
import com.learn.learnbackend.user.dto.UserResponse;

public interface UserService {

    UserResponse register(RegisterRequest request);

    UserResponse login(LoginRequest request);

    UserResponse getCurrentUser(Long userId);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);
}
