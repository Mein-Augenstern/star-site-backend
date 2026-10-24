package com.xp.starsite.member.vo;

/**
 * 登录方式视图（标识符已脱敏）：「我的登录方式」列表项。
 *
 * @param id           绑定关系 id
 * @param identityType 身份类型（WECHAT_MP/PASSWORD/EMAIL 等）
 * @param identifier   脱敏后的标识符（PASSWORD 明文回显，其余中段打码）
 * @param createdAt    绑定时间（ISO 字符串，可空）
 * @author muyi
 * @since 2026-10
 */
public record IdentityVO(Long id, String identityType, String identifier, String createdAt) {
}
