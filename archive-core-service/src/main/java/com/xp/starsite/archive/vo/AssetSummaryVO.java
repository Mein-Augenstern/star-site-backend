package com.xp.starsite.archive.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资产总览（家庭看板 / 财务三张表预热）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class AssetSummaryVO {

    @Data
    public static class TypeSlice {
        private String assetType;
        private BigDecimal totalValue;
        private long count;
    }

    /** 全家资产总估值 */
    private BigDecimal totalValue;

    /** 按类型分布 */
    private List<TypeSlice> slices;
}
