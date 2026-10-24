package com.xp.starsite.member.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录身份（member_identity，一个账号 N 种登录方式；多端归一）。
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class MemberIdentity {

    /** 主键 */
    private Long id;

    /** 所属账号 id */
    private Long memberId;

    /** WECHAT_MP / ALIPAY_MP / PASSWORD / PHONE / EMAIL */
    private String identityType;

    /** openid / 用户名 / 手机号 / 邮箱 */
    private String identifier;

    /** bcrypt 密码（PASSWORD）/ session_key（微信系，敏感不下发） */
    private String credential;

    /** 微信系跨端归一键 */
    private String unionid;

    /** 逻辑删除：1已删 0未删 */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
