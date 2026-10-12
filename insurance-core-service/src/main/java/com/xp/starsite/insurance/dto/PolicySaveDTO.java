package com.xp.starsite.insurance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 保单创建/编辑入参
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class PolicySaveDTO {

    @NotBlank(message = "保险公司不能为空")
    private String company;

    @NotBlank(message = "产品名不能为空")
    private String productName;

    private String policyNo;

    @NotBlank(message = "险种不能为空")
    private String category;

    @NotNull(message = "被保人不能为空")
    private Long insuredMemberId;

    private LocalDate startDate;

    private LocalDate maturityDate;

    @Min(value = 0, message = "保费不能为负")
    private BigDecimal premium;

    /** single/annual，默认 annual */
    private String paymentFreq;

    private LocalDate nextPaymentDate;

    private BigDecimal coverageAmount;

    /** 1有效 2缴费中断 3已满期 4已退保，默认 1 */
    private Integer status;

    private Long fileId;

    private String remark;
}
