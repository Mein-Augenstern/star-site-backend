package com.xp.starsite.health.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 体检报告指标（health_indicator）：报告单上的具体检验/检查项目。
 * report_year 冗余存储，趋势查询免 join。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@TableName("health_indicator")
public class HealthIndicator {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属报告 id（health_report.id） */
    private Long reportId;

    /** 家庭成员 id（自报告冗余） */
    private Long familyMemberId;

    /** 报告年份（冗余，趋势查询免 join） */
    private Integer reportYear;

    /** 指标名，如 空腹血糖 */
    private String itemName;

    /** 分组，如 血常规/肝功能 */
    private String itemGroup;

    /** 数值型结果 */
    private BigDecimal valueNum;

    /** 文本型结果（阴/阳、定性描述） */
    private String valueText;

    /** 结果单位（mmol/L、g/L...） */
    private String unit;

    /** 参考区间下限（可空） */
    private BigDecimal refLow;

    /** 参考区间上限（可空） */
    private BigDecimal refHigh;

    /** 0正常 1偏高 2偏低（按 value vs 参考区间自动判） */
    private Integer abnormalFlag;

    /** 报告单内展示序号（详情按此排序） */
    private Integer sortNo;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
