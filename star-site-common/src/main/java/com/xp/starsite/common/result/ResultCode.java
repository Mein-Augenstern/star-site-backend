package com.xp.starsite.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应码约定：0 成功；4xx 客户端错误；5xx 服务端错误
 *
 * @author muyi
 * @since 2026-10
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(0, "成功"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限"),

    USERNAME_EXISTS(4001, "用户名已被注册"),
    USERNAME_OR_PASSWORD_ERROR(4002, "用户名或密码错误"),
    ACCOUNT_DISABLED(4003, "账号已被禁用"),
    OLD_PASSWORD_ERROR(4004, "原密码错误"),

    SYSTEM_ERROR(500, "系统繁忙，请稍后再试");

    private final int code;
    private final String message;
}
