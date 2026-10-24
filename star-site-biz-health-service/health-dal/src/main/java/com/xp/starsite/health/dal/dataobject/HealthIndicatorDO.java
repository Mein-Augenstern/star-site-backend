package com.xp.starsite.health.dal.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 体检指标数据对象（health_indicator 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/HealthIndicator。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class HealthIndicatorDO {

    /** 主键 */
    private Long id;

    /** 所属报告 id */
    private Long reportId;

    /** 家庭成员 id（冗余，加速按人查询） */
    private Long familyMemberId;

    /** 报告年份（冗余） */
    private Integer reportYear;

    /** 指标名称 */
    private String itemName;

    /** 指标分组 */
    private String itemGroup;

    /** 数值结果 */
    private BigDecimal valueNum;

    /** 文本结果（阴/阳等） */
    private String valueText;

    /** 单位 */
    private String unit;

    /** 参考下限 */
    private BigDecimal refLow;

    /** 参考上限 */
    private BigDecimal refHigh;

    /** 异常标志（0 正常 1 偏高 2 偏低） */
    private Integer abnormalFlag;

    /** 排序号 */
    private Integer sortNo;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
