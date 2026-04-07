/**
 * 请求上下文：当前登录用户信息与 ThreadLocal 持有者。
 * <p>
 * 由 {@link org.hylee.phms.server.config.AuthInterceptor} 在请求进入时写入、请求结束时清理，
 * 供 service 层获取当前用户 ID 与权限信息。
 */
package org.hylee.phms.server.context;
