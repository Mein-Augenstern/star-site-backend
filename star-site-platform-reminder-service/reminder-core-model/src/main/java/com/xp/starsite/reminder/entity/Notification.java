package com.xp.starsite.reminder.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知（前端铃铛数据源；投递历史不删，无 deleted 列）。
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class Notification {

    /** 主键（DB 自增） */
    private Long id;

    private Long occurrenceId;

    private Long ownerMemberId;

    private String title;

    /** 0未读 1已读 */
    private Integer readFlag;

    private LocalDateTime createdAt;
}
