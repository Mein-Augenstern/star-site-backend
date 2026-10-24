package com.xp.starsite.member.mapper;

import com.xp.starsite.member.dal.dataobject.MemberIdentityDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * member_identity 登录身份表数据访问（DAL）：SQL 全部收敛在 mapper XML。
 * 逻辑删：member_identity.deleted，查询一律带 deleted=0，删除为 UPDATE 置位。
 * (identity_type, identifier) 唯一键 + unionid 跨端归一键；credential 落 session_key/BCrypt 密文（敏感不下发）。
 *
 * @author muyi
 * @since 2026-10
 */
@Mapper
public interface MemberIdentityMapper {

    /** 新建身份（credential=session_key/密码密文，可为 null；useGeneratedKeys 回填 id） */
    int insert(MemberIdentityDO identity);

    /** 按 id 取身份（含逻辑删过滤，不存在返回 null） */
    MemberIdentityDO selectById(@Param("id") Long id);

    /** 按身份类型+标识取身份（openid/邮箱等精确命中；登录与绑定链路，至多一条） */
    MemberIdentityDO selectByIdentity(@Param("identityType") String identityType,
                                      @Param("identifier") String identifier);

    /** 按 unionid 归一取身份（微信多端同主体认账号；至多一条） */
    MemberIdentityDO selectByUnionid(@Param("identityType") String identityType,
                                     @Param("unionid") String unionid);

    /** 某身份类型+标识的占用计数（邮箱绑定查重等） */
    long countByIdentity(@Param("identityType") String identityType,
                         @Param("identifier") String identifier);

    /** 某账号的全部登录身份（按 id 升序，「我的登录方式」列表） */
    List<MemberIdentityDO> selectByMemberId(@Param("memberId") Long memberId);

    /** 某账号的登录身份数量（解绑前"至少保留一种"防自锁校验） */
    long countByMemberId(@Param("memberId") Long memberId);

    /**
     * 身份刷新/改绑：仅更新非空字段（memberId/credential/unionid，
     * 与原 MyBatis-Plus updateById 的"null 字段不更新"语义一致；session_key 刷新走 credential 列）。
     */
    int update(MemberIdentityDO identity);

    /** 逻辑删：deleted 置 1（解绑登录方式） */
    int markDeleted(@Param("id") Long id);
}
