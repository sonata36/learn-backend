/**
 * 阶段 3 版本三：Token（JWT，建议 jjwt 库）。
 *
 * 要点：
 * - 签发有 exp 的 Access Token，只放必要声明（sub/iat/exp），不放敏感资料；
 * - 服务端校验签名、过期、声明、用户状态；
 * - Header（Authorization: Bearer xxx）天然免疫 CSRF，但 XSS 下可被读取；
 * - 无状态 Token 退出无法真正吊销，需黑名单或短有效期缓解。
 */
package com.learn.learnbackend.auth.token;
