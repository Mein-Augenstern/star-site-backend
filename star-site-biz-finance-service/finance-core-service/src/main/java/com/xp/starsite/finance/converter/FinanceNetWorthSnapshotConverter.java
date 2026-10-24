package com.xp.starsite.finance.converter;

import com.xp.starsite.finance.dal.dataobject.FinanceNetWorthSnapshotDO;
import com.xp.starsite.finance.entity.FinanceNetWorthSnapshot;
import org.springframework.beans.BeanUtils;

/**
 * 净资产快照模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author muyi
 * @since 2026-10
 */
public final class FinanceNetWorthSnapshotConverter {

    private FinanceNetWorthSnapshotConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static FinanceNetWorthSnapshotDO toDO(FinanceNetWorthSnapshot snapshot) {
        FinanceNetWorthSnapshotDO row = new FinanceNetWorthSnapshotDO();
        row.setId(snapshot.getId());
        row.setFamilyId(snapshot.getFamilyId());
        row.setOwnerMemberId(snapshot.getOwnerMemberId());
        row.setSnapDate(snapshot.getSnapDate());
        row.setTotalAsset(snapshot.getTotalAsset());
        row.setTotalLiability(snapshot.getTotalLiability());
        row.setNetWorth(snapshot.getNetWorth());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static FinanceNetWorthSnapshot toEntity(FinanceNetWorthSnapshotDO row) {
        FinanceNetWorthSnapshot snapshot = new FinanceNetWorthSnapshot();
        BeanUtils.copyProperties(row, snapshot);
        return snapshot;
    }
}
