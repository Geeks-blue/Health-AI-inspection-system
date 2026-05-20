package com.geeksblue.inspection.security;

/**
 * 简化版的角色定义。
 *
 * <p>当前通过请求头 X-User-Role 传递，生产环境应替换为 JWT 或会话鉴权。
 */
public enum Role {
    /** 学生：上传 */
    STUDENT,
    /** 老师：复核 */
    TEACHER,
    /** 管理员：统计 */
    ADMIN;

    /** 容错解析：未知角色一律视为学生（最低权限） */
    public static Role parse(String raw) {
        if (raw == null) return STUDENT;
        try {
            return Role.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return STUDENT;
        }
    }
}
