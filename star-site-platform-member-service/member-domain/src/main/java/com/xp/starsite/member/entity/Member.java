package com.xp.starsite.member.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员账号（member）：登录主体（用户名密码 + 微信身份两通道），与家庭成员（自然人）分离。
 * created_at / updated_at 由数据库默认值维护。
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class Member {

    /** 主键 */
    private Long id;

    /** 登录名 */
    private String username;

    /** BCrypt 密文 */
    private String password;

    /** 昵称（微信静默注册时缺省「微信用户+openid 尾号」） */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 邮箱（提醒邮件触达用，可空） */
    private String email;

    /** 手机号（可空） */
    private String phone;

    /** 0未知 1男 2女 */
    private Integer gender;

    /** 个人简介 */
    private String bio;

    /** 1正常 0禁用 */
    private Integer status;

    /** 逻辑删除：1已删 0未删 */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
