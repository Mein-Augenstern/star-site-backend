package com.xp.starsite.reminder.dal.dataobject;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 提醒规则数据对象（reminder_rule 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/ReminderRule。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class ReminderRuleDO {

    /** 主键 */
    private Long id;

    /** 家庭 id */
    private Long familyId;

    /** 家庭成员 id（用户自建规则时指定） */
    private Long familyMemberId;

    /** 归属账号 id（通知投递对象） */
    private Long ownerMemberId;

    /** 来源（USER 自建 / PREGNANCY_BRIDGE 桥接等） */
    private String source;

    /** 业务类型（如 PREGNANCY_CHECKUP） */
    private String bizType;

    /** 业务 id（幂等键第三段） */
    private Long bizId;

    /** 提醒标题 */
    private String title;

    /** 首次提醒日 */
    private LocalDate firstRemindAt;

    /** 重复类型（ONCE/WEEKLY/...） */
    private String repeatType;

    /** 重复截止日 */
    private LocalDate repeatUntil;

    /** 提前提醒天数 */
    private Integer advanceDays;

    /** 状态（1 启用 0 停用） */
    private Integer status;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
