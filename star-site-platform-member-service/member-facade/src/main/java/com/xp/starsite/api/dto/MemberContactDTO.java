package com.xp.starsite.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 会员联系方式快照（跨服务传输：提醒邮件触达用）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class MemberContactDTO implements Serializable {

    /** 账号 id（member.id） */
    private Long id;

    /** 昵称（提醒邮件称呼用） */
    private String nickname;

    /** 可能为空（注册时选填；为空则邮件渠道跳过该用户） */
    private String email;
}
