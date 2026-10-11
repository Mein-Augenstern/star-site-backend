package com.xp.starsite.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑个人资料入参
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class MemberUpdateDTO {

    @Size(max = 32, message = "昵称最长 32 位")
    private String nickname;

    @Size(max = 255, message = "头像 URL 最长 255 位")
    private String avatar;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱最长 128 位")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Min(value = 0, message = "gender 取值 0/1/2")
    @Max(value = 2, message = "gender 取值 0/1/2")
    private Integer gender;

    @Size(max = 255, message = "简介最长 255 位")
    private String bio;
}
