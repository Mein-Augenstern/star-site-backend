package com.xp.starsite.health.dal.dataobject;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体检报告数据对象（health_report 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/HealthReport。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class HealthReportDO {

    /** 主键 */
    private Long id;

    /** 家庭成员 id */
    private Long familyMemberId;

    /** 报告年份 */
    private Integer reportYear;

    /** 体检日期 */
    private LocalDate checkupDate;

    /** 医院 */
    private String hospital;

    /** 标题 */
    private String title;

    /** 总结 */
    private String summary;

    /** 报告文件 id */
    private Long fileId;

    /** 备注 */
    private String remark;

    /** AI 解读文本 */
    private String interpretation;

    /** AI 解读时间 */
    private LocalDateTime interpretedAt;

    /** 逻辑删标志 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
