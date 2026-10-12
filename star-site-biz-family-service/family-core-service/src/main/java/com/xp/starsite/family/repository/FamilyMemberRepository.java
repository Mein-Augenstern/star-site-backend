package com.xp.starsite.family.repository;

import com.xp.starsite.family.entity.FamilyMember;

import java.util.List;
import java.util.Optional;

/**
 * 家庭成员档案仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface FamilyMemberRepository {

    /** 保存（新增；回填自增 id） */
    FamilyMember save(FamilyMember member);

    /** 按 id 取（含逻辑删过滤） */
    Optional<FamilyMember> findById(Long id);

    /** 某家庭全部成员（按 id 升序：建档顺序即列表顺序） */
    List<FamilyMember> findByFamilyId(Long familyId);

    /** 成员档案编辑（仅非空字段落库） */
    void update(FamilyMember member);

    /** 逻辑删 */
    void markDeleted(Long id);
}
