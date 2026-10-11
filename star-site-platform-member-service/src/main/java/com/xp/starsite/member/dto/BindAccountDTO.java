package com.xp.starsite.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信身份绑定已有账号入参：账密校验通过后，将当前微信 openid 挂到该账号
 */
@Data
public class BindAccountDTO {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
