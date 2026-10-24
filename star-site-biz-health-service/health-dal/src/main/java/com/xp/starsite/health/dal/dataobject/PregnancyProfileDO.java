package com.xp.starsite.health.dal.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 孕期档案数据对象（pregnancy_profile 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/PregnancyProfile。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class PregnancyProfileDO {

    /** 主键 */
    private Long id;

    /** 家庭成员 id */
    private Long familyMemberId;

    /** 末次月经第一天 */
    private LocalDate lmpDate;

    /** 身高 cm */
    private BigDecimal heightCm;

    /** 孕前体重 kg */
    private BigDecimal prePregnancyWeight;

    /** 是否结束（0 在孕 1 已结束） */
    private Integer ended;

    /** 备注 */
    private String remark;

    /** 逻辑删标志 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
