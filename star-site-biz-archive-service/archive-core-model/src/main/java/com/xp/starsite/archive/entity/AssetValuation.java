package com.xp.starsite.archive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资产估值历史（archive_asset_valuation）：每次更新 current_value 自动追加一条，
 * 用于资产估值变化曲线。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@TableName("archive_asset_valuation")
public class AssetValuation {

    /** 主键 */
    @TableId(type = IdType.AUTO)
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
