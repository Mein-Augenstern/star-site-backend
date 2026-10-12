package com.xp.starsite.archive.converter;

import com.xp.starsite.archive.dal.dataobject.ArchiveAssetDO;
import com.xp.starsite.archive.entity.ArchiveAsset;
import org.springframework.beans.BeanUtils;

/**
 * 家庭资产模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class ArchiveAssetConverter {

    private ArchiveAssetConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static ArchiveAssetDO toDO(ArchiveAsset asset) {
        ArchiveAssetDO row = new ArchiveAssetDO();
        row.setId(asset.getId());
        row.setFamilyId(asset.getFamilyId());
        row.setOwnerMemberId(asset.getOwnerMemberId());
        row.setName(asset.getName());
        row.setAssetType(asset.getAssetType());
        row.setHolderMemberId(asset.getHolderMemberId());
        row.setHolderMemberName(asset.getHolderMemberName());
        row.setAcquireDate(asset.getAcquireDate());
        row.setCurrentValue(asset.getCurrentValue());
        row.setValuedAt(asset.getValuedAt());
        row.setNextInspectDate(asset.getNextInspectDate());
        row.setRemark(asset.getRemark());
        row.setDeleted(asset.getDeleted());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static ArchiveAsset toEntity(ArchiveAssetDO row) {
        ArchiveAsset asset = new ArchiveAsset();
        BeanUtils.copyProperties(row, asset);
        return asset;
    }
}
