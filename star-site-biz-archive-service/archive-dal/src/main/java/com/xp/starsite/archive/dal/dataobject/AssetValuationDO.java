package com.xp.starsite.archive.dal.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资产估值历史数据对象（asset_valuation 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/AssetValuation。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class AssetValuationDO {

    /** 主键（DB 自增） */
    private Long id;

    /** 所属资产 id */
    private Long assetId;

    /** 估值 */
    private BigDecimal value;

    /** 估值时点 */
    private LocalDateTime valuedAt;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
