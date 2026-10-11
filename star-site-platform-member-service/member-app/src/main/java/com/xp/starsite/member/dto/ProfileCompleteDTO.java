package com.xp.starsite.member.dto;

import lombok.Data;

/** 完善资料入参（微信静默注册后引导补充） */
@Data
public class ProfileCompleteDTO {
    private String nickname;
    private String avatar;
}
