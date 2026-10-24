package com.xp.starsite.archive.repository;

import com.xp.starsite.archive.entity.AssetValuation;

import java.util.List;

/**
 * 资产估值历史仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author muyi
 * @since 2026-10
 */
public interface AssetValuationRepository {

    /** 追加一条估值留痕（只追加不删改） */
    AssetValuation save(AssetValuation valuation);

    /** 某资产的估值曲线（按估值时点升序） */
    List<AssetValuation> findByAssetId(Long assetId);
}
