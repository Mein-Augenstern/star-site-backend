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
    /** 保险公司 */
    private String company;

    @NotBlank(message = "产品名不能为空")
    /** 产品名 */
    private String productName;

    /** 保单号 */
    private String policyNo;

    @NotBlank(message = "险种不能为空")
    /** 险种：critical_illness/major_medical/accident/life/annuity/vehicle/property/other */
    private String category;

    @NotNull(message = "被保人不能为空")
    /** 被保人（family 成员档案 id） */
    private Long insuredMemberId;

    /** 保障起始日 */
    private LocalDate startDate;

    /** 满期日（终身险可空） */
    private LocalDate maturityDate;

    @Min(value = 0, message = "保费不能为负")
    /** 年缴保费（元） */
    private BigDecimal premium;

    /** single/annual，默认 annual */
    private String paymentFreq;

    /** 下次缴费日（缴费提醒源） */
    private LocalDate nextPaymentDate;

    /** 保额（元） */
    private BigDecimal coverageAmount;

    /** 1有效 2缴费中断 3已满期 4已退保，默认 1 */
    private Integer status;

    /** 保单文件 id（file 服务），可空 */
    private Long fileId;

    /** 备注 */
    private String remark;
}
