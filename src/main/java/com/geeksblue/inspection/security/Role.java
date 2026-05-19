package com.geeksblue.inspection.security;

/**
 * Minimal role model. In production this should be backed by JWT or session auth;
 * here we read role + user id from request headers so the small footprint stays
 * within the scope of the design doc.
 */
public enum Role {
    STUDENT, TEACHER, ADMIN;

    public static Role parse(String raw) {
        if (raw == null) return STUDENT;
        try {
            return Role.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return STUDENT;
        }
    }
}
