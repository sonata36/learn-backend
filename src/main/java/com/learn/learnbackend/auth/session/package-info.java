/**
 * 阶段 3 版本二：Cookie + Session。
 *
 * 要点：
 * - Cookie 只放不可预测的 Session ID（SecureRandom，至少 128 位熵）；
 * - 用户身份/过期时间放服务端 Session（进程内 ConcurrentHashMap）；
 * - 登录成功换新 Session ID 防会话固定；
 * - 退出删除/失效 Session；
 * - 进程内 Session 应用重启即失效、多实例不共享（本轮不引入 Redis）。
 */
package com.learn.learnbackend.auth.session;
