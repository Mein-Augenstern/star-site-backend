package com.xp.starsite.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xp.starsite.member.entity.MemberIdentity;

/**
 * member_identity 登录身份表 Mapper：会员多登录方式（微信/账密/邮箱等）的持久化访问入口。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface MemberIdentityMapper extends BaseMapper<MemberIdentity> {
}
