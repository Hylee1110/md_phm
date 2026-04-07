package org.hylee.phms.server.context;

/**
 * 当前请求线程内的登录用户持有者（{@link ThreadLocal}）。
 * <p>
 * 必须在请求结束时调用 {@link #clear()}，防止线程池复用导致用户信息串线。
 */
public final class LoginUserHolder {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private LoginUserHolder() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser loginUser = HOLDER.get();
        return loginUser == null ? null : loginUser.userId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
