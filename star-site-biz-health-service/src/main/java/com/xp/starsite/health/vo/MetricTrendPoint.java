package com.xp.starsite.health.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 日常指标趋势点（按测量日期） */
public record MetricTrendPoint(LocalDate metricDate, BigDecimal valueNum) {
}
