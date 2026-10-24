package com.xp.starsite.member.repository;

import com.xp.starsite.member.entity.MemberIdentity;

import java.util.List;
import java.util.Optional;

/**
 * 登录身份仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author muyi
 * @since 2026-10
 */
public interface MemberIdentityRepository {

    /** 保存（新增；回填自增 id） */
    MemberIdentity save(MemberIdentity identity);

    /** 按 id 取（含逻辑删过滤） */
    Optional<MemberIdentity> findById(Long id);

    /** 按身份类型+标识取（openid/邮箱等精确命中，至多一条） */
    Optional<MemberIdentity> findByIdentity(String identityType, String identifier);

    /** 按 unionid 归一取（微信多端同主体认账号，至多一条） */
    Optional<MemberIdentity> findByUnionid(String identityType, String unionid);

    /** 某身份类型+标识的占用计数（邮箱绑定查重等） */
    long countByIdentity(String identityType, String identifier);

    /** 某账号的全部登录身份（「我的登录方式」列表） */
    List<MemberIdentity> findByMemberId(Long memberId);

    /** 某账号的登录身份数量（解绑前防自锁校验） */
    long countByMemberId(Long memberId);

    /** 身份刷新/改绑（仅非空字段落库） */
    void update(MemberIdentity identity);

    /** 逻辑删（解绑登录方式） */
    void markDeleted(Long id);
}
