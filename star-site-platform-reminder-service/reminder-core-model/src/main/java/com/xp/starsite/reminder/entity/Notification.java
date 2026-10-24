package com.xp.starsite.reminder.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知（前端铃铛数据源；投递历史不删，无 deleted 列）。
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class Notification {

    /** 主键（DB 自增） */
    private Long id;

    /** 所属提醒实例 id */
    private Long occurrenceId;

    /** 归属账号 id（铃铛/列表按账号取） */
    private Long ownerMemberId;

    /** 通知标题（冗余自实例） */
    private String title;

    /** 0未读 1已读 */
    private Integer readFlag;

    /** 创建时间（DB 维护，即投递时间） */
    private LocalDateTime createdAt;
}
