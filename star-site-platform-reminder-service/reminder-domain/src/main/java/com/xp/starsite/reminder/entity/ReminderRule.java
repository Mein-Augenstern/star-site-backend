package com.xp.starsite.reminder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 提醒规则（事件消费端 upsert，撤销物理删——衍生物无历史价值，唯一键才干净）
 */
@Data
@TableName("reminder_rule")
public class ReminderRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long familyId;

    /** 提醒归属成员（用户自建规则；业务事件规则为空） */
    private Long familyMemberId;

    private Long ownerMemberId;

    /** 来源服务，如 star-site-insurance */
    private String source;

    /** 业务类型，如 policy_payment */
    private String bizType;

    private Long bizId;

    private String title;

    private LocalDate firstRemindAt;

    /** NONE/ANNUAL/MONTHLY/WEEKLY/CUSTOM */
    private String repeatType;

    /** 重复截止日，null = 无限 */
    private LocalDate repeatUntil;

    private Integer advanceDays;

    /** 1启用 0停用 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
