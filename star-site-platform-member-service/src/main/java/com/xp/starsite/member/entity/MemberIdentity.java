package com.xp.starsite.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 登录身份（一个账号 N 种登录方式；多端归一） */
@Data
@TableName("member_identity")
public class MemberIdentity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memberId;

    /** WECHAT_MP / ALIPAY_MP / PASSWORD / PHONE / EMAIL */
    private String identityType;

    /** openid / 用户名 / 手机号 / 邮箱 */
    private String identifier;

    /** bcrypt 密码（PASSWORD）/ session_key（微信系，敏感不下发） */
    private String credential;

    /** 微信系跨端归一键 */
    private String unionid;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
