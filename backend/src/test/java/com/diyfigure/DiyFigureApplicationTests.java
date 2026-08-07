package com.diyfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Spring Boot 上下文加载测试
 *
 * 注意:此测试需要 MySQL 运行在 localhost:3306
 * 如果没有 MySQL,可以使用 H2 内存数据库(需添加 H2 依赖)
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/diy_figure?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
        "spring.jpa.hibernate.ddl-auto=update"
})
class DiyFigureApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文能正常加载
    }
}
