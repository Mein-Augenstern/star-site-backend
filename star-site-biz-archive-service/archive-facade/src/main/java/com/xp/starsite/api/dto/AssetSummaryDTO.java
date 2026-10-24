package com.xp.starsite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 资产总览快照（跨服务传输：财务资产负债表的资产侧取数）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetSummaryDTO {

    /** 全家资产总估值（archive 域：房产/车辆/存款理财台账） */
    private BigDecimal totalValue;
}
