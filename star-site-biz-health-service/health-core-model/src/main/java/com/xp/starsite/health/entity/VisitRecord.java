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
 * 就医记录
 */
@Data
@TableName("visit_record")
public class VisitRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 家庭成员 id（family 服务的自然人档案） */
    private Long familyMemberId;

    /** 就诊日期 */
    private LocalDate visitDate;

    private String hospital;

    /** 科室 */
    private String department;

    /** 诊断 */
    private String diagnosis;

    /** 用药/处方摘要 */
    private String medication;

    /** 费用（元） */
    private BigDecimal cost;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
