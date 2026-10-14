package com.xp.starsite.reminder.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 用户自建提醒规则视图
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class UserRuleVO {

    /** 主键 */
    private Long id;

    /** 提醒归属成员 */
    private Long familyMemberId;

    /** 提醒标题 */
    private String title;

    /** 首次提醒日 */
    private LocalDate firstRemindAt;

    /** 重复类型：NONE/ANNUAL/MONTHLY/WEEKLY/DAILY */
    private String repeatType;

    /** 重复截止日（可空=长期） */
    private LocalDate repeatUntil;

    /** 1启用 0停用 */
    private Integer status;

    /** 未来 N 天内待触发实例数 */
    private Long pendingCount;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
