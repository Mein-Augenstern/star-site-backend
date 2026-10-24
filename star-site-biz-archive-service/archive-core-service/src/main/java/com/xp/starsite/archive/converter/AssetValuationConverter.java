package com.xp.starsite.archive.converter;

import com.xp.starsite.archive.dal.dataobject.AssetValuationDO;
import com.xp.starsite.archive.entity.AssetValuation;
import org.springframework.beans.BeanUtils;

/**
 * 资产估值历史模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 *
 * @author muyi
 * @since 2026-10
 */
public final class AssetValuationConverter {

    private AssetValuationConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static AssetValuationDO toDO(AssetValuation valuation) {
        AssetValuationDO row = new AssetValuationDO();
        row.setId(valuation.getId());
        row.setAssetId(valuation.getAssetId());
        row.setValue(valuation.getValue());
        row.setValuedAt(valuation.getValuedAt());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static AssetValuation toEntity(AssetValuationDO row) {
        AssetValuation valuation = new AssetValuation();
        BeanUtils.copyProperties(row, valuation);
        return valuation;
    }
}
