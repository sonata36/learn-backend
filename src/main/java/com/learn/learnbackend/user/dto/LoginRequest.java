package com.learn.learnbackend.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /** 用户名或邮箱 */
    @NotBlank(message = "用户名或邮箱不能为空")
    private String login;

    @NotBlank(message = "密码不能为空")
    private String password;
}
