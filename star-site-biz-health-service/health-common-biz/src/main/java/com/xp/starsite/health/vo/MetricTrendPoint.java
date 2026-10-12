package com.xp.starsite.health.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 日常指标趋势点（按测量日期）
 *
 * @author xiaopu
 * @since 2026-10
 */
public record MetricTrendPoint(LocalDate metricDate, BigDecimal valueNum) {
}
