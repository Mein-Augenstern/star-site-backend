package com.xp.starsite.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册入参
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class RegisterDTO {

    /** 登录名（全局唯一，字母/数字/下划线，3-32 位） */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度须为 3-32 位")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字、下划线")
    private String username;

    /** 登录密码（明文，6-64 位，服务端 BCrypt 加密落库） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度须为 6-64 位")
    private String password;

    /** 昵称（选填，空则默认取登录名） */
    @Size(max = 32, message = "昵称最长 32 位")
    private String nickname;
}
