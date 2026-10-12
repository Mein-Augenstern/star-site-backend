package com.xp.starsite.member.dal.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录身份数据对象（member_identity 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/MemberIdentity。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class MemberIdentityDO {

    /** 主键 */
    private Long id;

    /** 所属会员 id */
    private Long memberId;

    /** 身份类型（WECHAT_MP/PASSWORD/EMAIL，见 IdentityType 常量） */
    private String identityType;

    /** 标识符（openid/用户名/邮箱，类型内唯一） */
    private String identifier;

    /** 凭证（session_key/BCrypt 密文，敏感不下发） */
    private String credential;

    /** 微信 unionid（多端同主体归一键） */
    private String unionid;

    /** 逻辑删标志（0 正常 1 已解绑） */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
