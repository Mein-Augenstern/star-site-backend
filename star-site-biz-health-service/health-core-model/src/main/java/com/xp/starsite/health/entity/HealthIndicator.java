package com.xp.starsite.health.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 体检指标（report_year 冗余存储，趋势查询免 join）
 */
@Data
@TableName("health_indicator")
public class HealthIndicator {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reportId;

    private Long familyMemberId;

    private Integer reportYear;

    /** 指标名，如 空腹血糖 */
    private String itemName;

    /** 分组，如 血常规/肝功能 */
    private String itemGroup;

    /** 数值型结果 */
    private BigDecimal valueNum;

    /** 文本型结果（阴/阳、定性描述） */
    private String valueText;

    private String unit;

    private BigDecimal refLow;

    private BigDecimal refHigh;

    /** 0正常 1偏高 2偏低（按 value vs 参考区间自动判） */
    private Integer abnormalFlag;

    private Integer sortNo;

    private LocalDateTime createdAt;
}
