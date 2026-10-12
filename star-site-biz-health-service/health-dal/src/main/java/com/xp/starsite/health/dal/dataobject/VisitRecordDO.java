package com.xp.starsite.health.dal.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 就医记录数据对象（visit_record 表的镜像映射，DAL 自持——不依赖任何上层模型）。
 * 字段与表列一一对应；领域语义见 core-model 的 {@link com.xp.starsite.health.entity.VisitRecord}。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class VisitRecordDO {

    /** 主键 */
    private Long id;

    /** 家庭成员 id */
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
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
