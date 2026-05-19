package com.geeksblue.inspection.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 教室元数据。教室列表由系统在启动时种入，可供前端展示和占用锁定。
 */
@Entity
@Table(name = "classroom")
public class Classroom {

    /** 教室编号，例如 A101 */
    @Id
    @Column(length = 64)
    private String id;

    /** 教室显示名（可选），例如「一楼东 A101」 */
    @Column(length = 128)
    private String name;

    public Classroom() {}

    public Classroom(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
