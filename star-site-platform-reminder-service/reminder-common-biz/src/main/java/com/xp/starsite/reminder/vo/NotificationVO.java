package com.xp.starsite.reminder.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知视图（铃铛下拉）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class NotificationVO {

    /** 主键 */
    private Long id;

    /** 所属提醒实例 id */
    private Long occurrenceId;

    /** 通知标题（冗余自实例） */
    private String title;

    /** 0未读 1已读 */
    private Integer readFlag;

    /** 创建时间（即投递时间） */
    private LocalDateTime createdAt;
}
