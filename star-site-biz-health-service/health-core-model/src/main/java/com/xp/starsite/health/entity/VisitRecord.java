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
 * 就医记录（visit_record）：门诊/住院/产检等就诊事件台账。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@TableName("visit_record")
public class VisitRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 家庭成员 id（family 服务的自然人档案） */
    private Long familyMemberId;

    /** 就诊日期 */
    private LocalDate visitDate;

    /** 就诊医院 */
    private String hospital;

    /** 科室 */
    private String department;

    /** 诊断 */
    private String diagnosis;

    /** 用药/处方摘要 */
    private String medication;

    /** 费用（元） */
    private BigDecimal cost;

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
