package com.xp.starsite.insurance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保单视图
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class PolicyVO {

    /** 保单 id */
    private Long id;

    /** 保险公司 */
    private String company;

    /** 产品名 */
    private String productName;

    /** 保单号 */
    private String policyNo;

    /** 险种：critical_illness/major_medical/accident/life/annuity/vehicle/property/other */
    private String category;

    /** 被保人（family 成员档案 id） */
    private Long insuredMemberId;

    /** 被保人姓名快照（免联表） */
    private String insuredMemberName;

    /** 保障起始日 */
    private LocalDate startDate;

    /** 满期日（终身险为 null） */
    private LocalDate maturityDate;

    /** 年缴保费（元） */
    private BigDecimal premium;

    /** 缴费频率：annual/monthly/... */
    private String paymentFreq;

    /** 下次缴费日 */
    private LocalDate nextPaymentDate;

    /** 保额（元） */
    private BigDecimal coverageAmount;

    /** 状态：1有效 2已终止... */
    private Integer status;

    /** 保单文件 id（file 服务） */
    private Long fileId;

    /** 备注 */
    private String remark;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
