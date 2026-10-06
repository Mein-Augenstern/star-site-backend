package com.xp.starsite.member.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录出参
 */
@Data
@Builder
public class LoginVO {

    private String token;

    /** 有效期（分钟） */
    private Long expiresIn;

    private MemberVO member;
}
