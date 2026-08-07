package com.diyfigure.entity;

import com.diyfigure.common.enums.DesignStatus;
import com.diyfigure.common.enums.SpecTier;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 系列表 - 设计容器(对应 03-技术设计说明书.md 第 5.2 节)
 *
 * 职责:只负责设计阶段的数据(名称、档位、画布集合)
 * 生命周期:从创建到设计完成
 *
 * 注意:系列与订单的职责拆分是核心设计决策(03 文档第 4 节)
 *      series 只管设计,order 管交易状态机
 */
@Entity
@Table(name = "series")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Series {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 用户自命名的系列名称 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 规格档位:LIGHT(6选4) / CLASSIC(9选6) / COLLECTION(12选8) */
    @Enumerated(EnumType.STRING)
    @Column(name = "spec_tier", nullable = false, length = 20)
    private SpecTier specTier;

    /** 尺寸档位:对应不同大小包装盒,具体规格待厂家回填后配置化管理 */
    @Column(name = "size_tier", nullable = false, length = 50)
    private String sizeTier;

    /** 设计状态:DESIGNING / READY_FOR_QUOTE */
    @Enumerated(EnumType.STRING)
    @Column(name = "design_status", nullable = false, length = 30)
    private DesignStatus designStatus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
