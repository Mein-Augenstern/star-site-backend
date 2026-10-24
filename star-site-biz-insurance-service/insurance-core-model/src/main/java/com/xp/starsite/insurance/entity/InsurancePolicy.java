package com.xp.starsite.insurance.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保单（insurance_policy）：全家保单档案的主体表。
 * 缴费提醒以 next_payment_date 为源（ANNUAL 重复），到期提醒以 maturity_date 为源。
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class InsurancePolicy {

    /** 雪花 id（应用侧赋值）：事务消息需要在 insert 前拿到 id 填进事件载荷 */
    private Long id;

    /** 家庭 id（多人协作预留冗余） */
    private Long familyId;

    /** 持有账号 id（member 服务） */
    private Long ownerMemberId;

    /** 保险公司 */
    private String company;

    /** 产品名 */
    private String productName;

    /** 保单号 */
    private String policyNo;

    /** 险种：critical_illness/major_medical/accident/life/annuity/vehicle/property/other */
    private String category;

    /** 被保人（family 服务自然人档案） */
    private Long insuredMemberId;

    /** 被保人姓名快照（免联表） */
    private String insuredMemberName;

    /** 保障起始日 */
    private LocalDate startDate;

    /** 满期日，null = 终身 */
    private LocalDate maturityDate;

    /** 年保费（趸交为总保费） */
    private BigDecimal premium;

    /** 缴费频率：single/annual */
    private String paymentFreq;

    /** 下次缴费日（缴费提醒源） */
    private LocalDate nextPaymentDate;

    /** 保额 */
    private BigDecimal coverageAmount;

    /** 1有效 2缴费中断 3已满期 4已退保 */
    private Integer status;

    /** 电子保单附件（file 服务） */
    private Long fileId;

    /** 备注 */
    private String remark;

    /** 逻辑删标志（0 正常 1 已删） */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
