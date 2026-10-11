package com.xp.starsite.health.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 孕期档案（每家庭成员至多一条） */
@Data
@TableName("pregnancy_profile")
public class PregnancyProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long familyMemberId;

    /** 末次月经第一天 */
    private LocalDate lmpDate;

    /** 身高 cm */
    private BigDecimal heightCm;

    /** 孕前体重 kg */
    private BigDecimal prePregnancyWeight;

    /** 0 孕期中 1 已结束 */
    private Integer ended;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
