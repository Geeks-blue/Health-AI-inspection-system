package com.geeksblue.inspection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 教室卫生 AI 智能巡查后端启动类。
 *
 * <p>@EnableScheduling 用于开启 7 天图片清理的定时任务（详见 PhotoCleanupJob）。
 */
@SpringBootApplication
@EnableScheduling
public class InspectionApplication {

    /** 程序入口。 */
    public static void main(String[] args) {
        SpringApplication.run(InspectionApplication.class, args);
    }
}
