package com.xp.starsite.archive.repository.impl;

import com.xp.starsite.archive.converter.ArchiveAssetConverter;
import com.xp.starsite.archive.dal.dataobject.ArchiveAssetDO;
import com.xp.starsite.archive.entity.ArchiveAsset;
import com.xp.starsite.archive.mapper.ArchiveAssetMapper;
import com.xp.starsite.archive.projection.AssetTypeSummary;
import com.xp.starsite.archive.repository.ArchiveAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 家庭资产仓储实现：调用 dal 的 {@link ArchiveAssetMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class ArchiveAssetRepositoryImpl implements ArchiveAssetRepository {

    private final ArchiveAssetMapper assetMapper;

    @Override
    public ArchiveAsset save(ArchiveAsset asset) {
        ArchiveAssetDO row = ArchiveAssetConverter.toDO(asset);
        assetMapper.insert(row);
        asset.setId(row.getId());
        return asset;
    }

    @Override
    public Optional<ArchiveAsset> findById(Long id) {
        return Optional.ofNullable(assetMapper.selectById(id)).map(ArchiveAssetConverter::toEntity);
    }

    @Override
    public void update(ArchiveAsset asset) {
        assetMapper.update(ArchiveAssetConverter.toDO(asset));
    }

    @Override
    public void markDeleted(Long id) {
        assetMapper.markDeleted(id);
    }

    @Override
    public long countByOwner(Long ownerMemberId, String assetType) {
        return assetMapper.countByOwner(ownerMemberId, assetType);
    }

    @Override
    public List<ArchiveAsset> pageByOwner(Long ownerMemberId, String assetType, long offset, int limit) {
        return assetMapper.selectPage(ownerMemberId, assetType, offset, limit)
                .stream().map(ArchiveAssetConverter::toEntity).toList();
    }

    @Override
    public BigDecimal totalValue(Long ownerMemberId) {
        return assetMapper.selectTotalValue(ownerMemberId);
    }

    @Override
    public List<AssetTypeSummary> typeSummaries(Long ownerMemberId) {
        return assetMapper.selectTypeSummaries(ownerMemberId);
    }
}
