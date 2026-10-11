package com.xp.starsite.health.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体检报告（health_report）：一次体检的元数据档案，指标明细在 {@link HealthIndicator}。
 * 删除为逻辑删（deleted）；AI 解读文本落本表缓存（interpretation/interpretedAt）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@TableName("health_report")
public class HealthReport {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 家庭成员 id（family 服务的自然人档案） */
    private Long familyMemberId;

    /** 报告年份（列表过滤与指标趋势的横轴） */
    private Integer reportYear;

    /** 体检日期 */
    private LocalDate checkupDate;

    /** 体检机构 */
    private String hospital;

    /** 报告标题（缺省「&lt;年份&gt;年体检报告」） */
    private String title;

    /** 体检总结（人工录入或 OCR 摘要） */
    private String summary;

    /** 报告文件 id（file 服务，原报告 PDF/图片） */
    private Long fileId;

    /** 备注 */
    private String remark;

    /** AI 解读文本（未解读为 null；异步解读完成后回填，详情接口轮询此字段） */
    private String interpretation;

    /** 解读时间 */
    private LocalDateTime interpretedAt;

    /** 逻辑删标志（0 正常 1 已删） */
    @TableLogic
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
