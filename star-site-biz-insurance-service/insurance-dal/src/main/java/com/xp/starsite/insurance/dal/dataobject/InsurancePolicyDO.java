package com.xp.starsite.insurance.dal.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保单数据对象（insurance_policy 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/InsurancePolicy。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class InsurancePolicyDO {

    /** 主键（雪花 id，应用侧赋值） */
    private Long id;

    /** 家庭 id */
    private Long familyId;

    /** 持有账号 id */
    private Long ownerMemberId;

    /** 保险公司 */
    private String company;

    /** 产品名 */
    private String productName;

    /** 保单号 */
    private String policyNo;

    /** 险种（critical_illness/major_medical/...） */
    private String category;

    /** 被保家庭成员 id */
    private Long insuredMemberId;

    /** 被保成员姓名快照 */
    private String insuredMemberName;

    /** 生效日 */
    private LocalDate startDate;

    /** 满期日（终身险为 null） */
    private LocalDate maturityDate;

    /** 年缴保费 */
    private BigDecimal premium;

    /** 缴费频率（annual/monthly/...） */
    private String paymentFreq;

    /** 下次缴费日 */
    private LocalDate nextPaymentDate;

    /** 保额 */
    private BigDecimal coverageAmount;

    /** 状态（1 有效 2 已终止 ...） */
    private Integer status;

    /** 保单文件 id（file 服务） */
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
