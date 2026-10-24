package com.xp.starsite.reminder.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 提醒规则（事件消费端 upsert，幂等键 (source,bizType,bizId) 唯一；撤销物理删——
 * 衍生物无历史价值，唯一键才干净，故本表无 deleted 列）。
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class ReminderRule {

    /** 主键（DB 自增） */
    private Long id;

    /** 家庭 id */
    private Long familyId;

    /** 提醒归属成员（用户自建规则；业务事件规则为空） */
    private Long familyMemberId;

    /** 归属账号 id（通知投递对象） */
    private Long ownerMemberId;

    /** 来源服务，如 star-site-insurance */
    private String source;

    /** 业务类型，如 policy_payment */
    private String bizType;

    /** 业务 id（幂等键第三段；用户自建规则回填规则自身 id） */
    private Long bizId;

    /** 提醒标题 */
    private String title;

    /** 首次提醒日 */
    private LocalDate firstRemindAt;

    /** NONE/ANNUAL/MONTHLY/WEEKLY/CUSTOM */
    private String repeatType;

    /** 重复截止日，null = 无限 */
    private LocalDate repeatUntil;

    /** 提前提醒天数 */
    private Integer advanceDays;

    /** 1启用 0停用 */
    private Integer status;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
