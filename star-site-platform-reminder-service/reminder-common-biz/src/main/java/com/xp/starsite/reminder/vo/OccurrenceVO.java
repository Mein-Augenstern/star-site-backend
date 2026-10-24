package com.xp.starsite.reminder.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 提醒实例视图（ upcoming 列表 / "最近要办的事"）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class OccurrenceVO {

    /** 主键 */
    private Long id;

    /** 标题（冗余自规则） */
    private String title;

    /** 来源服务（前端按它跳转对应页面） */
    private String source;

    /** 业务类型 */
    private String bizType;

    /** 业务 id */
    private Long bizId;

    /** 应提醒日 */
    private LocalDate dueDate;

    /** 1待提醒 2已通知 3已完成 4已忽略 */
    private Integer status;

    /** 通知时间（投递完成置位） */
    private LocalDateTime notifiedAt;
}
