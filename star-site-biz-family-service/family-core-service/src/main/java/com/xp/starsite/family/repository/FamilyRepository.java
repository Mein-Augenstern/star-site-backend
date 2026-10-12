package com.xp.starsite.family.repository;

import com.xp.starsite.family.entity.Family;

import java.util.Optional;

/**
 * 家庭仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface FamilyRepository {

    /** 保存（新增；回填自增 id） */
    Family save(Family family);

    /** 按 id 取（含逻辑删过滤） */
    Optional<Family> findById(Long id);

    /** 按顶梁柱账号取家庭（一账号一家庭，至多一条） */
    Optional<Family> findByOwner(Long ownerMemberId);

    /** 顶梁柱建家计数（防重复创建） */
    long countByOwner(Long ownerMemberId);
}
