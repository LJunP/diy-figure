package com.diyfigure.common.enums;

import lombok.Getter;

/**
 * 系列规格档位
 *
 * 对应 02-产品需求说明书.md 第 5 节关键业务规则:
 * - 轻量(LIGHT): 设计 6 个,定制 4 个,未中签 2 个可补购
 * - 经典(CLASSIC): 设计 9 个,定制 6 个,未中签 3 个可补购
 * - 收藏(COLLECTION): 设计 12 个,定制 8 个,未中签 4 个可补购
 *
 * 保留比例约 2/3
 */
@Getter
public enum SpecTier {

    /** 轻量系列:6 选 4 */
    LIGHT(6, 4),
    /** 经典系列:9 选 6 */
    CLASSIC(9, 6),
    /** 收藏系列:12 选 8 */
    COLLECTION(12, 8);

    /** 需要设计的画布总数 */
    private final int designCount;

    /** 抽奖选中的数量(实际生产数量) */
    private final int selectedCount;

    SpecTier(int designCount, int selectedCount) {
        this.designCount = designCount;
        this.selectedCount = selectedCount;
    }

    /**
     * 未中签数量(可补购数量)
     */
    public int getNotSelectedCount() {
        return designCount - selectedCount;
    }
}
