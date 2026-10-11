package com.xp.starsite.reminder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 提醒实例（扫描从规则预生成，(rule_id, due_date) 唯一防重）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@TableName("reminder_occurrence")
public class ReminderOccurrence {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;

    private Long familyId;

    private Long ownerMemberId;

    /** 标题冗余自规则：规则撤销后已生成实例与通知不丢 */
    private String title;

    private String source;

    private String bizType;

    private Long bizId;

    private LocalDate dueDate;

    /** 1待提醒 2已通知 3已完成 4已忽略 */
    private Integer status;

    private LocalDateTime notifiedAt;

    private LocalDateTime createdAt;
}
