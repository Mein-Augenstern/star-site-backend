package com.xp.starsite.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保单
 */
@Data
@TableName("insurance_policy")
public class InsurancePolicy {

    /** 雪花 id（应用侧赋值）：事务消息需要在 insert 前拿到 id 填进事件载荷 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 家庭 id（多人协作预留冗余） */
    private Long familyId;

    /** 持有账号 id（member 服务） */
    private Long ownerMemberId;

    private String company;

    private String productName;

    private String policyNo;

    /** critical_illness/major_medical/accident/life/annuity/vehicle/property/other */
    private String category;

    /** 被保人（family 服务自然人档案） */
    private Long insuredMemberId;

    /** 被保人姓名快照（免联表） */
    private String insuredMemberName;

    private LocalDate startDate;

    /** 满期日，null = 终身 */
    private LocalDate maturityDate;

    /** 年保费（趸交为总保费） */
    private BigDecimal premium;

    /** single/annual */
    private String paymentFreq;

    /** 下次缴费日（提醒源） */
    private LocalDate nextPaymentDate;

    private BigDecimal coverageAmount;

    /** 1有效 2缴费中断 3已满期 4已退保 */
    private Integer status;

    /** 电子保单附件（file 服务） */
    private Long fileId;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
