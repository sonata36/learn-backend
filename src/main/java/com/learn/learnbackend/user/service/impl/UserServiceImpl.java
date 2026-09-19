package com.learn.learnbackend.user.service.impl;

import com.learn.learnbackend.common.BusinessException;
import com.learn.learnbackend.common.ErrorCode;
import com.learn.learnbackend.user.dto.ChangePasswordRequest;
import com.learn.learnbackend.user.dto.LoginRequest;
import com.learn.learnbackend.user.dto.RegisterRequest;
import com.learn.learnbackend.user.dto.UpdateProfileRequest;
import com.learn.learnbackend.user.dto.UserResponse;
import com.learn.learnbackend.user.entity.User;
import com.learn.learnbackend.user.repository.UserRepository;
import com.learn.learnbackend.user.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // 预检用于友好提示；真正兜底的是数据库唯一约束
        if (userRepository.findByUsername(request.getUsername()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        if (userRepository.findByEmail(request.getEmail()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(1);
        try {
            userRepository.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发下“先查再插”会漏，靠唯一约束最终拦截
            throw new BusinessException(ErrorCode.CONFLICT, "用户名或邮箱已存在");
        }
        return UserResponse.from(requireById(user.getId()));
    }

    @Override
    public UserResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.getLogin());
        // 统一错误信息，不暴露“用户不存在”与“密码错误”的差异（内部日志可区分）
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        return UserResponse.from(user);
    }

    @Override
    public UserResponse getCurrentUser(Long userId) {
        return UserResponse.from(requireById(userId));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireById(userId);
        User existing = userRepository.findByEmail(request.getEmail());
        if (existing != null && !existing.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已被使用");
        }
        user.setEmail(request.getEmail());
        try {
            userRepository.updateProfile(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已被使用");
        }
        return UserResponse.from(requireById(userId));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = requireById(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "旧密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.updatePassword(user);
    }

    private User requireById(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }
}
