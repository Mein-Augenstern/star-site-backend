package com.xp.starsite.reminder.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知视图（铃铛下拉）
 */
@Data
public class NotificationVO {

    private Long id;

    private Long occurrenceId;

    private String title;

    /** 0未读 1已读 */
    private Integer readFlag;

    private LocalDateTime createdAt;
}
