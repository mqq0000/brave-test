package com.bravetest.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录用户上下文（由 JwtInterceptor 写入）
 */
public class UserContext {

    @Data
    @AllArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String username;
        private String role;
    }

    private static final ThreadLocal<UserInfo> HOLDER = new ThreadLocal<>();

    public static void set(UserInfo info) {
        HOLDER.set(info);
    }

    public static UserInfo get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        UserInfo info = HOLDER.get();
        if (info == null) {
            throw new BusinessException(401, "未登录或会话已过期");
        }
        return info.getUserId();
    }

    public static String getRole() {
        UserInfo info = HOLDER.get();
        return info == null ? null : info.getRole();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
