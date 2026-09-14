package com.diyfigure.entity;

import jakarta.persistence.Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 乐观锁保护测试
 *
 * 并发入口(抽签、支付回调、补购、定稿)依赖 @Version 防止"两个请求读到同一份旧状态"，
 * 一旦有人误删这个注解,这里会立刻失败。
 *
 * 注意:这是结构性保护,真正的并发行为由集成测试覆盖。
 */
class OptimisticLockTest {

    /** 需要并发保护的实体 */
    private static final List<Class<?>> PROTECTED_ENTITIES = List.of(
            OrderEntity.class,
            OrderCanvas.class,
            Payment.class,
            Canvas.class,
            Series.class
    );

    @Test
    @DisplayName("关键实体都带有乐观锁版本字段")
    void criticalEntitiesHaveVersion() {
        for (Class<?> entity : PROTECTED_ENTITIES) {
            boolean hasVersion = Arrays.stream(entity.getDeclaredFields())
                    .anyMatch(f -> f.isAnnotationPresent(Version.class));
            assertTrue(hasVersion, entity.getSimpleName() + " 缺少 @Version 乐观锁字段");
        }
    }

    @Test
    @DisplayName("版本字段名统一为 version(与 V2 迁移脚本一致)")
    void versionFieldNameIsConsistent() {
        for (Class<?> entity : PROTECTED_ENTITIES) {
            var field = Arrays.stream(entity.getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(Version.class))
                    .findFirst();
            assertTrue(field.isPresent());
            assertNotNull(field.get().getAnnotation(Version.class));
            assertTrue(
                    "version".equals(field.get().getName())
                            || field.get().getAnnotation(jakarta.persistence.Column.class) == null
                            || "version".equals(field.get().getAnnotation(jakarta.persistence.Column.class).name()),
                    entity.getSimpleName() + " 的乐观锁字段必须映射为 version 列");
        }
    }
}
