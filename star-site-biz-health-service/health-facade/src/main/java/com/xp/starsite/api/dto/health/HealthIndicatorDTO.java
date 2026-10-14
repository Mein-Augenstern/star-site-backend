package com.xp.starsite.api.dto.health;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 体检指标快照（跨服务传输用）：health_indicator 单行投影。
 * abnormalFlag: 0正常 1偏高 2偏低；valueNum/valueText 二选一有值。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class HealthIndicatorDTO implements Serializable {

    private Long id;

    private Long reportId;

    private Integer reportYear;

    /** 指标名（如 空腹血糖），复查闭环按此对齐基线 */
    private String itemName;

    /** 指标分组（如 血脂四项） */
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
