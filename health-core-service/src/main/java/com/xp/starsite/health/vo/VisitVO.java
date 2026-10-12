package com.xp.starsite.health.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 就医记录视图
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class VisitVO {

    private Long id;

    private Long familyMemberId;

    private LocalDate visitDate;

    private String hospital;

    private String department;

    private String diagnosis;

    private String medication;

    private BigDecimal cost;

    private String remark;

    private LocalDateTime createdAt;
}
