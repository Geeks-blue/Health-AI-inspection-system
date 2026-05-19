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
        List<Classroom> seeds = List.of(
                new Classroom("A101", "A 楼 101"),
                new Classroom("A102", "A 楼 102"),
                new Classroom("A103", "A 楼 103"),
                new Classroom("B201", "B 楼 201"),
                new Classroom("B202", "B 楼 202"),
                new Classroom("C301", "C 楼 301")
        );
        repository.saveAll(seeds);
        log.info("种入演示教室共 {} 间", seeds.size());
    }
}
