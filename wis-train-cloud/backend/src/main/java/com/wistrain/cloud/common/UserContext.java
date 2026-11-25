package com.wistrain.cloud.common;

/**
 * 当前登录用户上下文（由 AuthInterceptor 写入，Controller/Service 读取）
 */
public class UserContext {

    public record UserInfo(Long userId, Integer role, Long deptId, String realname) {}

    private static final ThreadLocal<UserInfo> HOLDER = new ThreadLocal<>();

    public static void set(UserInfo info) {
        HOLDER.set(info);
    }

    public static UserInfo get() {
        UserInfo u = HOLDER.get();
        if (u == null) {
            throw new BizException(401, "未登录");
        }
        return u;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
