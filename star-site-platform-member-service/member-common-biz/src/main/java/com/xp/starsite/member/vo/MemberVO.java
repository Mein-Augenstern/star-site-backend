package com.xp.starsite.member.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员对外视图（不含密码）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class MemberVO {

    /** 账号 id */
    private Long id;

    /** 登录名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 邮箱（提醒邮件触达用，可空） */
    private String email;

    /** 手机号（可空） */
    private String phone;

    /** 性别：0未知 1男 2女 */
    private Integer gender;

    /** 个人简介 */
    private String bio;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
