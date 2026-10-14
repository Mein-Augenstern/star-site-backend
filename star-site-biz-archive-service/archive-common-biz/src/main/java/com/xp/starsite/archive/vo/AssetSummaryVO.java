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
        /** 资产类型：real_estate/vehicle/deposit/investment/fund/other */
        private String assetType;

        /** 该类型资产估值合计（元） */
        private BigDecimal totalValue;

        /** 该类型资产件数 */
        private long count;
    }

    /** 全家资产总估值 */
    private BigDecimal totalValue;

    /** 按类型分布 */
    private List<TypeSlice> slices;
}
