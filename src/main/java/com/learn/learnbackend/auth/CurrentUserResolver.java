package com.learn.learnbackend.auth;

import com.learn.learnbackend.common.BusinessException;
import com.learn.learnbackend.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 把 Controller 方法参数上的 CurrentUser 自动解析出来。
 *
 * 阶段 2（临时实现）：从 X-User-Id 请求头解析当前用户 id，便于先跑通“登录后才能访问”。
 * 阶段 3（替换实现）：改为从 Cookie / Session / Token 中解析真实登录凭据，
 *                    并把凭据校验（签名/过期/用户状态）收敛到这里或独立的 AuthService。
 */
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CurrentUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String userIdHeader = request == null ? null : request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        try {
            long id = Long.parseLong(userIdHeader.trim());
            return new CurrentUser(id, "user-" + id);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录凭据无效");
        }
    }
}
