package com.diyfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring Boot 上下文加载测试
 *
 * 用 test  profile 而不是 dev,原因有两点:
 * 1. 不依赖开发库 diy_figure,CI 上只需一个 MySQL + 空 schema 就能跑
 * 2. ddl-auto=validate + Flyway —— 这个用例同时是「迁移脚本与 JPA 实体是否一致」的守卫:
 *    只要 db/migration 建出来的表和实体对不上,这里必然启动失败
 *
 * 本地运行前先建库:
 *   CREATE DATABASE diy_figure_it DEFAULT CHARACTER SET utf8mb4;
 * 再执行:
 *   mvn test -DTEST_DB_PASSWORD=你的root密码
 */
@SpringBootTest
@ActiveProfiles("test")
class DiyFigureApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文能正常加载,且 Flyway 建表结果与实体定义一致
    }
}
