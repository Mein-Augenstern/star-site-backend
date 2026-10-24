package com.xp.starsite.health.repository;

import com.xp.starsite.health.entity.PregnancyProfile;

import java.util.Optional;

/**
 * 孕期档案仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author muyi
 * @since 2026-10
 */
public interface PregnancyProfileRepository {

    /** 按家庭成员取档案（一人一条；无档案返回 empty） */
    Optional<PregnancyProfile> findByFamilyMemberId(Long familyMemberId);

    /** 保存（新增或更新；一人一条，存在则更新） */
    PregnancyProfile save(PregnancyProfile profile);
}
