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

    private Long id;

    private Long familyMemberId;

    private String title;

    private LocalDate firstRemindAt;

    private String repeatType;

    private LocalDate repeatUntil;

    private Integer status;

    /** 已生成未完成的实例数（近期待办量） */
    private Long pendingCount;

    private LocalDateTime createdAt;
}
