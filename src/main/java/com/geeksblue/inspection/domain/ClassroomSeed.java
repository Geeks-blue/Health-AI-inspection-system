package com.geeksblue.inspection.domain;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时为空库种入一组演示教室，方便前端立刻有数据可点。
 * 生产环境可改为读配置文件 / 后台维护。
 */
@Component
public class ClassroomSeed {

    private static final Logger log = LoggerFactory.getLogger(ClassroomSeed.class);

    private final ClassroomRepository repository;

    public ClassroomSeed(ClassroomRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void seed() {
        if (repository.count() > 0) {
            return;
        }
        // cameraUrl 为占位演示值；生产替换为真实摄像头 snapshot URL，或后台维护
        List<Classroom> seeds = List.of(
                new Classroom("A101", "A 楼 101", "http://camera.example.com/A101/snapshot.jpg"),
                new Classroom("A102", "A 楼 102", "http://camera.example.com/A102/snapshot.jpg"),
                new Classroom("A103", "A 楼 103", null),
                new Classroom("B201", "B 楼 201", "http://camera.example.com/B201/snapshot.jpg"),
                new Classroom("B202", "B 楼 202", "http://camera.example.com/B202/snapshot.jpg"),
                new Classroom("C301", "C 楼 301", null)
        );
        repository.saveAll(seeds);
        log.info("种入演示教室共 {} 间", seeds.size());
    }
}
