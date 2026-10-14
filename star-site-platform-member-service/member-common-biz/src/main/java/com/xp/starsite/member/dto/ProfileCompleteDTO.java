package com.xp.starsite.member.dto;

import lombok.Data;

/** 完善资料入参（微信静默注册后引导补充）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class ProfileCompleteDTO {
    /** 昵称（替换微信静默注册的缺省值「微信用户+openid 尾号」） */
    private String nickname;

    /** 头像 URL */
    private String avatar;
}
