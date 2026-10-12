package com.xp.starsite.reminder.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 提醒实例（扫描从规则预生成，(rule_id, due_date) 唯一防重；随规则撤销物理删，无 deleted 列）。
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class ReminderOccurrence {

    /** 主键（DB 自增） */
    private Long id;

    /** 所属规则 id */
    private Long ruleId;

    /** 家庭 id */
    private Long familyId;

    /** 归属账号 id（通知投递对象） */
    private Long ownerMemberId;

    /** 标题冗余自规则：规则撤销后已生成实例与通知不丢 */
    private String title;

    /** 来源（与规则一致，如 star-site-insurance） */
    private String source;

    /** 业务类型（与规则一致） */
    private String bizType;

    /** 业务 id（与规则一致） */
    private Long bizId;

    /** 应提醒日 */
    private LocalDate dueDate;

    /** 1待提醒 2已通知 3已完成 4已忽略 */
    private Integer status;

    /** 通知时间（投递完成置位） */
    private LocalDateTime notifiedAt;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
