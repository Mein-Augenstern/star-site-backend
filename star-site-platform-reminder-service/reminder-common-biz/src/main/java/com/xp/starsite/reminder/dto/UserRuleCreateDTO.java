package com.xp.starsite.reminder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 用户自建提醒规则入参（用药/复查/疫苗等）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class UserRuleCreateDTO {

    /** 提醒归属成员 */
    @NotNull(message = "家庭成员不能为空")
    private Long familyMemberId;

    /** 提醒标题 */
    @NotBlank(message = "提醒标题不能为空")
    @Size(max = 128, message = "标题最长 128 位")
    private String title;

    /** 首次提醒日 */
    @NotNull(message = "首次提醒日期不能为空")
    private LocalDate firstRemindAt;

    /** NONE/WEEKLY/MONTHLY/ANNUAL，缺省 NONE */
    @Pattern(regexp = "NONE|WEEKLY|MONTHLY|ANNUAL", message = "重复策略不合法")
    private String repeatType;

    /** 重复截止日（可空=长期） */
    private LocalDate repeatUntil;
}
