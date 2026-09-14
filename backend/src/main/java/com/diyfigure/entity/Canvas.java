package com.diyfigure.entity;

import com.diyfigure.common.enums.CanvasStatus;
import com.diyfigure.common.enums.Model3dStatus;
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

    /**
     * 角色名称,由用户在创建画布时填写
     * 订单详情、补购列表等展示位统一用它,不再用 "画布 {id}" 硬编码
     */
    @Column(nullable = false, length = 100)
    private String name;

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
     * 3D 生成任务 ID(Meshy 返回),用于轮询与重试
     * 定稿不再同步等待,任务由后台调度推进
     */
    @Column(name = "model_3d_task_id", length = 100)
    private String model3dTaskId;

    /**
     * 3D 生成状态,见 {@link com.diyfigure.common.enums.Model3dStatus}
     * 前端据此显示"生成中/失败可重试",而不是一直转圈
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "model_3d_status", nullable = false, length = 20)
    @Builder.Default
    private Model3dStatus model3dStatus = Model3dStatus.NONE;

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

    /** 乐观锁:防止定稿与并发对话同时写入导致状态回退 */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
