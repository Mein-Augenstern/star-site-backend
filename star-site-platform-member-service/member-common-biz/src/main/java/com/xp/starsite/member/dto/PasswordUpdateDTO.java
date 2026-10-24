package com.xp.starsite.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码入参
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class PasswordUpdateDTO {

    /** 原密码（明文，校验身份用，服务端 BCrypt 比对） */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /** 新密码（明文，6-64 位，服务端 BCrypt 加密落库） */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "新密码长度须为 6-64 位")
    private String newPassword;
}
