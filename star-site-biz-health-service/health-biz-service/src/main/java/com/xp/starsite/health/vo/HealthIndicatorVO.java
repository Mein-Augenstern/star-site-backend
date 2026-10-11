package com.xp.starsite.health.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 体检指标视图
 */
@Data
public class HealthIndicatorVO {

    private Long id;

    private Long reportId;

    private Integer reportYear;

    private String itemName;

    private String itemGroup;

    private BigDecimal valueNum;

    private String valueText;

    private String unit;

    private BigDecimal refLow;

    private BigDecimal refHigh;

    /** 0正常 1偏高 2偏低 */
    private Integer abnormalFlag;

    private Integer sortNo;

    private LocalDateTime createdAt;
}
