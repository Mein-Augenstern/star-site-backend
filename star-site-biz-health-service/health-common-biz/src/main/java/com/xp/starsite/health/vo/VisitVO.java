package com.xp.starsite.health.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 就医记录视图
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class VisitVO {

    /** 主键 */
    private Long id;

    /** 家庭成员 id */
    private Long familyMemberId;

    /** 就诊日期 */
    private LocalDate visitDate;

    /** 就诊医院 */
    private String hospital;

    /** 科室 */
    private String department;

    /** 诊断 */
    private String diagnosis;

    /** 用药/处方摘要 */
    private String medication;

    /** 费用（元） */
    private BigDecimal cost;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
