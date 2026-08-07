package com.diyfigure.entity;

import com.diyfigure.common.enums.CanvasStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 画布表 - 单个角色设计(对应 03-技术设计说明书.md 第 5.3 节)
 *
 * 一个画布 = 一个原创角色的 AI 对话式设计
 * 包含:2D 三视图概念图、3D 参考模型、AI 对话记录
 *
 * 画布状态:设计中 → 已定稿
 * 定金支付成功后 locked=true,已中签+未中签画布全部锁定,不可修改
 */
@Entity
@Table(name = "canvas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Canvas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属系列 ID */
    @Column(name = "series_id", nullable = false)
    private Long seriesId;

    /** 画布状态:DESIGNING / FINALIZED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanvasStatus status;

    /**
     * 2D 三视图概念图(正/侧/背),存 OSS 地址数组
     * JSON 类型,Hibernate 6 自动序列化/反序列化
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "concept_image_urls", columnDefinition = "json")
    @Builder.Default
    private List<String> conceptImageUrls = new ArrayList<>();

    /**
     * 3D 参考模型 .glb 文件 OSS 地址,定稿后生成
     * 非直接可打印文件,供厂家原型师精修参考
     */
    @Column(name = "model_3d_url", length = 500)
    private String model3dUrl;

    /**
     * AI 对话记录(JSON)
     * 存储文字内容 + 图片/文件的 OSS 引用
     * 格式:[{role:"user", content:"...", images:["oss://..."]}, {role:"assistant", content:"..."}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_conversation", columnDefinition = "json")
    @Builder.Default
    private List<Map<String, Object>> aiConversation = new ArrayList<>();

    /** 标记定稿时间 */
    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    /**
     * 定金支付成功后置 true,之后不可编辑
     * 覆盖已中签 + 未中签画布(原因:未中签画布代表已过审的潜在补购品,
     * 允许修改会重新触发合规审核链路)
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean locked = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
