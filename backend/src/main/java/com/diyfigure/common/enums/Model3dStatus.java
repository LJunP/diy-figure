package com.diyfigure.common.enums;

/**
 * 3D 参考模型生成状态
 *
 * 定稿不再同步等待 Meshy(最长 5 分钟),改为:
 * PENDING(待提交) → PROCESSING(生成中) → SUCCESS / FAILED
 * 由定时任务推进,失败可通过接口受控重试。
 */
public enum Model3dStatus {
    /** 未触发(未定稿或没有概念图) */
    NONE,
    /** 已入队,等待定时任务提交给 Meshy */
    PENDING,
    /** 已提交,等待轮询结果 */
    PROCESSING,
    /** 生成成功,model3dUrl 可用 */
    SUCCESS,
    /** 生成失败,可重试 */
    FAILED
}
