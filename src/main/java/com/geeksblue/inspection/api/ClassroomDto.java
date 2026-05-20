package com.geeksblue.inspection.api;

/**
 * 教室视图，带当前占用状态。
 */
public class ClassroomDto {

    private String id;
    private String name;
    /** 是否被占用 */
    private boolean locked;
    /** 占用人 ID（仅锁存在时） */
    private String lockedBy;
    /** 占用过期时间 ISO 字符串（仅锁存在时） */
    private String lockExpiresAt;

    public ClassroomDto() {}

    public ClassroomDto(String id, String name, boolean locked, String lockedBy, String lockExpiresAt) {
        this.id = id;
        this.name = name;
        this.locked = locked;
        this.lockedBy = lockedBy;
        this.lockExpiresAt = lockExpiresAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }

    public String getLockExpiresAt() { return lockExpiresAt; }
    public void setLockExpiresAt(String lockExpiresAt) { this.lockExpiresAt = lockExpiresAt; }
}
