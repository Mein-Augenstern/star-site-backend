package com.xp.starsite.health.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 孕期档案（pregnancy_profile）：每家庭成员至多一条。
 * 孕周/预产期/产检里程碑等计算视图不入库，见 PregnancyVO。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@TableName("pregnancy_profile")
public class PregnancyProfile {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 家庭成员 id（family 服务的自然人档案，一人一条） */
    private Long familyMemberId;

    /** 末次月经第一天（孕周与预产期的计算锚点） */
    private LocalDate lmpDate;

    /** 身高 cm（用于孕前 BMI） */
    private BigDecimal heightCm;

    /** 孕前体重 kg（用于孕前 BMI 与增重建议） */
    private BigDecimal prePregnancyWeight;

    /** 0 孕期中 1 已结束 */
    private Integer ended;

    /** 备注 */
    private String remark;

    /** 逻辑删标志（0 正常 1 已删） */
    @TableLogic
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
