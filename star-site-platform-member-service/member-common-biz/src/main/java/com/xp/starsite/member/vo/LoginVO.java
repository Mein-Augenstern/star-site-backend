package com.xp.starsite.member.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录出参
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@Builder
public class LoginVO {

    /** 登录令牌（JWT，载荷含 member.id/username，后续请求 Authorization: Bearer 携带） */
    private String token;

    /** 有效期（分钟） */
    private Long expiresIn;

    /** 当前登录会员视图（不含密码） */
    private MemberVO member;
}
