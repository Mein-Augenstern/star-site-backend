package com.xp.starsite.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录入参
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class LoginDTO {

    /** 登录名（即 member.username） */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 登录密码（明文，服务端 BCrypt 校验比对） */
    @NotBlank(message = "密码不能为空")
    private String password;
}
