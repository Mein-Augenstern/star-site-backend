package com.xp.starsite.archive.repository.impl;

import com.xp.starsite.archive.converter.AssetValuationConverter;
import com.xp.starsite.archive.dal.dataobject.AssetValuationDO;
import com.xp.starsite.archive.entity.AssetValuation;
import com.xp.starsite.archive.mapper.AssetValuationMapper;
import com.xp.starsite.archive.repository.AssetValuationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 资产估值历史仓储实现：调用 dal 的 {@link AssetValuationMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class AssetValuationRepositoryImpl implements AssetValuationRepository {

    private final AssetValuationMapper valuationMapper;

    @Override
    public AssetValuation save(AssetValuation valuation) {
        AssetValuationDO row = AssetValuationConverter.toDO(valuation);
        valuationMapper.insert(row);
        valuation.setId(row.getId());
        return valuation;
    }

    @Override
    public List<AssetValuation> findByAssetId(Long assetId) {
        return valuationMapper.selectByAssetId(assetId)
                .stream().map(AssetValuationConverter::toEntity).toList();
    }
}
