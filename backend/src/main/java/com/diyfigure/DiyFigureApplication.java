package com.diyfigure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DIY 定制盲盒手办平台 - 后端启动类
 *
 * @EnableScheduling: 启用定时任务,用于补购窗口到期检查(Phase 7)
 */
@SpringBootApplication
@EnableScheduling
public class DiyFigureApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiyFigureApplication.class, args);
    }
}
