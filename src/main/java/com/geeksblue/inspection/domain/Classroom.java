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

    /**
     * 监控摄像头快照 URL（可选）；为空表示该教室没有可用监控。
     * 推荐使用 IP 摄像头/NVR 提供的 HTTP snapshot 端点（直接返回一帧 JPEG）。
     * 例如 海康：http://user:pass@ip/ISAPI/Streaming/channels/101/picture
     *      大华：http://user:pass@ip/cgi-bin/snapshot.cgi
     */
    @Column(name = "camera_url", length = 512)
    private String cameraUrl;

    public Classroom() {}

    public Classroom(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Classroom(String id, String name, String cameraUrl) {
        this.id = id;
        this.name = name;
        this.cameraUrl = cameraUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCameraUrl() { return cameraUrl; }
    public void setCameraUrl(String cameraUrl) { this.cameraUrl = cameraUrl; }
}
