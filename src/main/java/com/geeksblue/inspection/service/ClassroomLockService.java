package com.geeksblue.inspection.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 教室占用锁：保证同一时间一间教室只能被一个用户「选中」。
 *
 * <p>实现要点：
 * <ul>
 *   <li>用 ConcurrentHashMap.compute 做原子性的「不存在/已过期则占用」。</li>
 *   <li>带 TTL（默认 5 分钟），防止用户选完不上传把教室锁死。</li>
 *   <li>持有者可主动释放；非持有者的释放被忽略。</li>
 *   <li>持有者再次 claim 同一间会顺延 TTL，避免长时间编辑被踢。</li>
 * </ul>
 */
@Service
public class ClassroomLockService {

    /** 单个锁的元数据 */
    public record Lock(String userId, Instant expiresAt) {
        public boolean isActive(Instant now) {
            return now.isBefore(expiresAt);
        }
    }

    /** 占用结果：成功（包含 Lock）或冲突（包含当前持有人） */
    public record ClaimOutcome(boolean ok, Lock current) {}

    private final Map<String, Lock> locks = new ConcurrentHashMap<>();
    private final Duration ttl;

    public ClassroomLockService(@Value("${cleaning.classroom-lock-ttl-seconds:300}") long ttlSeconds) {
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    /** 当前活跃的锁，过期项不返回 */
    public Lock get(String classroomId) {
        Lock l = locks.get(classroomId);
        if (l == null) return null;
        if (!l.isActive(Instant.now())) {
            locks.remove(classroomId, l);
            return null;
        }
        return l;
    }

    /** 尝试占用 */
    public ClaimOutcome claim(String classroomId, String userId) {
        Instant now = Instant.now();
        Lock taken = locks.compute(classroomId, (id, existing) -> {
            // 不存在 / 已过期 / 自己占的：覆盖并续期
            if (existing == null || !existing.isActive(now) || existing.userId().equals(userId)) {
                return new Lock(userId, now.plus(ttl));
            }
            return existing;
        });
        return new ClaimOutcome(taken.userId().equals(userId), taken);
    }

    /** 释放：只允许持有者释放 */
    public void release(String classroomId, String userId) {
        locks.compute(classroomId, (id, existing) -> {
            if (existing == null) return null;
            if (existing.userId().equals(userId)) return null;
            // 其它人持有 → 不动
            return existing;
        });
    }
}
