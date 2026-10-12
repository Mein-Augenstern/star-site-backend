package com.xp.starsite.archive.repository;

import com.xp.starsite.archive.entity.ArchiveAsset;
import com.xp.starsite.archive.projection.AssetTypeSummary;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 家庭资产仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 * {@link AssetTypeSummary} 为 dal 聚合查询的只读投影（非领域实体），经仓储透出。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface ArchiveAssetRepository {

    /** 保存（新增；雪花 id 应用侧已赋值） */
    ArchiveAsset save(ArchiveAsset asset);

    /** 按 id 取（含逻辑删过滤） */
    Optional<ArchiveAsset> findById(Long id);

    /** 显式更新资产全量业务列 */
    void update(ArchiveAsset asset);

    /** 逻辑删 */
    void markDeleted(Long id);

    /** 资产分页计数：归属账号 + 可选类型 */
    long countByOwner(Long ownerMemberId, String assetType);

    /** 资产分页：按 id 倒序（最新录入在前） */
    List<ArchiveAsset> pageByOwner(Long ownerMemberId, String assetType, long offset, int limit);

    /** 全部资产总估值（NULL 估值按 0 计；财务资产负债表聚合的数据源） */
    BigDecimal totalValue(Long ownerMemberId);

    /** 按资产类型分组汇总（总估值 + 件数；总览饼图数据源） */
    List<AssetTypeSummary> typeSummaries(Long ownerMemberId);
}
