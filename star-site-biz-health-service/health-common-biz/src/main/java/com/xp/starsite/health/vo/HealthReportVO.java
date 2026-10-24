package com.xp.starsite.health.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 体检报告视图（详情时 indicators 非空）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class HealthReportVO {

    /** 主键（报告 id） */
    private Long id;

    /** 家庭成员 id */
    private Long familyMemberId;

    /** 报告年份 */
    private Integer reportYear;

    /** 体检日期 */
    private LocalDate checkupDate;

    /** 医院 */
    private String hospital;

    /** 标题 */
    private String title;

    /** 总结 */
    private String summary;

    /** 报告文件 id（file 服务） */
    private Long fileId;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 详情接口返回 */
    private List<HealthIndicatorVO> indicators;
}
