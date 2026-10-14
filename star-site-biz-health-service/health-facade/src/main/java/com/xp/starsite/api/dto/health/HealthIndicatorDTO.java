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

    /** 主键（指标 id） */
    private Long id;

    /** 所属报告 id */
    private Long reportId;

    /** 报告年份（冗余，便于按年筛选展示） */
    private Integer reportYear;

    /** 指标名（如 空腹血糖），复查闭环按此对齐基线 */
    private String itemName;

    /** 指标分组（如 血脂四项） */
    private String itemGroup;

    /** 数值结果（与 valueText 二选一有值；定性指标为 null） */
    private BigDecimal valueNum;

    /** 文本结果（阴/阳等，与 valueNum 二选一有值；定量指标为 null） */
    private String valueText;

    /** 单位（如 mmol/L；仅定量指标有值） */
    private String unit;

    /** 参考区间下限（无下限为 null，展示可用 refRangeText） */
    private BigDecimal refLow;

    /** 参考区间上限（无上限为 null，展示可用 refRangeText） */
    private BigDecimal refHigh;

    /** 0正常 1偏高 2偏低 */
    private Integer abnormalFlag;

    /** 排序号（报告内展示顺序） */
    private Integer sortNo;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
