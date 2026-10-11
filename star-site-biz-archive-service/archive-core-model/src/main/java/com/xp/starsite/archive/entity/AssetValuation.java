package com.xp.starsite.archive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资产估值历史（每次更新 current_value 自动追加一条）
 */
@Data
@TableName("archive_asset_valuation")
public class AssetValuation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assetId;

    private BigDecimal value;

    /** 估值时点 */
    private LocalDateTime valuedAt;

    private LocalDateTime createdAt;
}
