package com.xp.starsite.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信身份绑定已有账号入参：账密校验通过后，将当前微信 openid 挂到该账号
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class BindAccountDTO {

    /** 登录名（即 member.username，账密校验通过后绑定微信身份） */
    @NotBlank(message = "账号不能为空")
    private String username;

    /** 登录密码（明文，服务端 BCrypt 校验比对） */
    @NotBlank(message = "密码不能为空")
    private String password;
}
