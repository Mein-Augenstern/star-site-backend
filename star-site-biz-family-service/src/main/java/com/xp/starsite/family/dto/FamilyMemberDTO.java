package com.xp.starsite.family.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 成员档案新增/编辑入参
 */
@Data
public class FamilyMemberDTO {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名最长 32 位")
    private String name;

    @NotBlank(message = "关系不能为空")
    @Pattern(regexp = "self|spouse|father|mother|child|other", message = "关系取值 self/spouse/father/mother/child/other")
    private String relation;

    @Min(value = 0, message = "gender 取值 0/1/2")
    @Max(value = 2, message = "gender 取值 0/1/2")
    private Integer gender;

    private LocalDate birthDate;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 头像文件 id（star-site-file） */
    private Long avatarFileId;

    /** 绑定登录账号 id，可空 */
    private Long linkedMemberId;

    @Size(max = 255, message = "备注最长 255 位")
    private String remark;
}
