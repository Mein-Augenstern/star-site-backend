package com.xp.starsite.member.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员对外视图（不含密码）
 */
@Data
public class MemberVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private Integer gender;

    private String bio;

    private LocalDateTime createdAt;
}
