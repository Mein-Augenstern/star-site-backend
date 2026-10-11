package com.xp.starsite.archive.projection;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 资产类型汇总行（DAL 查询投影）：GROUP BY asset_type 的总估值与数量。
 * 纯 POJO——列别名在 archive-dal 的 mapper XML 里对齐本类属性（驼峰自动映射）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class AssetTypeSummary {

    /** 资产类型：real_estate/vehicle/deposit/investment/fund/other */
    private String assetType;

    /** 该类型总估值（NULL 估值按 0 计） */
    private BigDecimal totalValue;

    /** 该类型资产件数 */
    private Long count;
}
