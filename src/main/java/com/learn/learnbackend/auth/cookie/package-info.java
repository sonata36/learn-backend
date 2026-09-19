/**
 * 阶段 3 版本一：签名 Cookie。
 *
 * 要点：
 * - 不得把明文 userId/username/isAdmin 直接当身份；
 * - 用 HMAC-SHA256 签名（javax.crypto.Mac，密钥来自配置），内容形如 userId.expireAt.signature；
 * - Cookie 属性：HttpOnly、Secure（生产）、SameSite=Lax、Max-Age；
 * - 写操作需处理 CSRF（SameSite + 校验 Origin/Referer 或 CSRF Token）；
 * - 退出只能删 Cookie，已签发值到期前被窃取仍可重放（README 说明）。
 */
package com.learn.learnbackend.auth.cookie;
