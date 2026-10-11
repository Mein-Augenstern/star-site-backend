package com.xp.starsite.insurance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保单视图
 */
@Data
public class PolicyVO {

    private Long id;

    private String company;

    private String productName;

    private String policyNo;

    private String category;

    private Long insuredMemberId;

    private String insuredMemberName;

    private LocalDate startDate;

    private LocalDate maturityDate;

    private BigDecimal premium;

    private String paymentFreq;

    private LocalDate nextPaymentDate;

    private BigDecimal coverageAmount;

    private Integer status;

    private Long fileId;

    private String remark;

    private LocalDateTime createdAt;
}
