package com.xp.starsite.member.repository;

import com.xp.starsite.member.entity.Member;

import java.util.Optional;

/**
 * 会员主表仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author muyi
 * @since 2026-10
 */
public interface MemberRepository {

    /** 保存（新增；回填自增 id） */
    Member save(Member member);

    /** 按 id 取（含逻辑删过滤） */
    Optional<Member> findById(Long id);

    /** 按用户名取（登录链路，至多一条） */
    Optional<Member> findByUsername(String username);

    /** 用户名占用计数（注册查重） */
    long countByUsername(String username);

    /** 资料更新（仅非空字段落库） */
    void updateProfile(Member member);

    /** 密码变更（BCrypt 密文整列替换） */
    void updatePassword(Long id, String password);
}
