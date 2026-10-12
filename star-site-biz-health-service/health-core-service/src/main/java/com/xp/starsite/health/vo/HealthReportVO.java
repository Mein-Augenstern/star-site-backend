package com.xp.starsite.health.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 体检报告视图（详情时 indicators 非空）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class HealthReportVO {

    private Long id;

    private Long familyMemberId;

    private Integer reportYear;

    private LocalDate checkupDate;

    private String hospital;

    private String title;

    private String summary;

    private Long fileId;

    private String remark;

    private LocalDateTime createdAt;

    /** 详情接口返回 */
    private List<HealthIndicatorVO> indicators;
}
