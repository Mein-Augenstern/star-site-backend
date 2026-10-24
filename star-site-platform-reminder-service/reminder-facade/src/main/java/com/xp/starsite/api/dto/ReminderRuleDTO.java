package com.xp.starsite.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 提醒规则（跨服务 upsert 语义：source+bizType+bizId 幂等）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class ReminderRuleDTO implements Serializable {

    /** 来源服务（如 star-site-biz-health） */
    private String source;

    /** 业务类型（如 prenatal_checkup） */
    private String bizType;

    /** 业务主键 */
    private Long bizId;

    /** 家庭 id */
    private Long familyId;

    /** 提醒归属成员（可空） */
    private Long familyMemberId;

    /** 提醒标题 */
    private String title;

    /** 首次提醒日期 */
    private LocalDate remindDate;

    /** 重复类型：NONE / ANNUAL / MONTHLY / WEEKLY / DAILY */
    private String repeatType;

    /** 归属账号（提醒接收人） */
    private Long ownerMemberId;

    /** 提前提醒天数 */
    private Integer advanceDays;

    /** 重复截止日（可空=无限） */
    private LocalDate repeatUntil;

    /** 备注说明 */
    private String remark;
}
