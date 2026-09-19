package com.learn.learnbackend.auth;

/**
 * 当前登录身份对象。
 * Todo 模块唯一依赖的身份信息，只含 id、username 等稳定字段，
 * 不依赖 HttpSession、HttpServletRequest，也不依赖用户模块的 UserRepository。
 *
 * 阶段 2：由 {@link CurrentUserResolver} 从 X-User-Id 请求头临时解析；
 * 阶段 3：替换为从 Cookie / Session / Token 中解析真实登录凭据。
 */
public record CurrentUser(Long id, String username) {
}
