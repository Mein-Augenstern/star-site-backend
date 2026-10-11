package com.xp.starsite.archive.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资产估值历史（archive_asset_valuation）：每次更新 current_value 自动追加一条，
 * 用于资产估值变化曲线。
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class AssetValuation {

    /** 主键（DB 自增） */
    private Long id;

    /** 资产 id（archive_asset.id） */
    private Long assetId;

    /** 本次估值 */
    private BigDecimal value;

    /** 估值时点 */
    private LocalDateTime valuedAt;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
