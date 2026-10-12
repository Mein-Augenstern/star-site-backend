package com.xp.starsite.reminder.dal.dataobject;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 提醒实例数据对象（reminder_occurrence 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/ReminderOccurrence。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class ReminderOccurrenceDO {

    /** 主键 */
    private Long id;

    /** 所属规则 id */
    private Long ruleId;

    /** 家庭 id */
    private Long familyId;

    /** 归属账号 id（通知投递对象） */
    private Long ownerMemberId;

    /** 提醒标题 */
    private String title;

    /** 来源 */
    private String source;

    /** 业务类型 */
    private String bizType;

    /** 业务 id */
    private Long bizId;

    /** 应提醒日 */
    private LocalDate dueDate;

    /** 状态（1 待提醒 2 已通知 3 已完成 4 已忽略） */
    private Integer status;

    /** 通知时间 */
    private LocalDateTime notifiedAt;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
