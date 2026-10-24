package com.xp.starsite.finance.converter;

import com.xp.starsite.finance.dal.dataobject.FinanceImportLogDO;
import com.xp.starsite.finance.entity.FinanceImportLog;
import org.springframework.beans.BeanUtils;

/**
 * 账单导入记录模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author muyi
 * @since 2026-10
 */
public final class FinanceImportLogConverter {

    private FinanceImportLogConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static FinanceImportLogDO toDO(FinanceImportLog importLog) {
        FinanceImportLogDO row = new FinanceImportLogDO();
        row.setId(importLog.getId());
        row.setOwnerMemberId(importLog.getOwnerMemberId());
        row.setAccountId(importLog.getAccountId());
        row.setFileId(importLog.getFileId());
        row.setChannel(importLog.getChannel());
        row.setParsed(importLog.getParsed());
        row.setImported(importLog.getImported());
        row.setDuplicates(importLog.getDuplicates());
        row.setSkipped(importLog.getSkipped());
        row.setStatus(importLog.getStatus());
        row.setError(importLog.getError());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static FinanceImportLog toEntity(FinanceImportLogDO row) {
        FinanceImportLog importLog = new FinanceImportLog();
        BeanUtils.copyProperties(row, importLog);
        return importLog;
    }
}
