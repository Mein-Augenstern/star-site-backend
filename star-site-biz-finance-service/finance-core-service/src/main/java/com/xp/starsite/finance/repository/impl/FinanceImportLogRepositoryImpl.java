package com.xp.starsite.finance.repository.impl;

import com.xp.starsite.finance.converter.FinanceImportLogConverter;
import com.xp.starsite.finance.dal.dataobject.FinanceImportLogDO;
import com.xp.starsite.finance.entity.FinanceImportLog;
import com.xp.starsite.finance.mapper.FinanceImportLogMapper;
import com.xp.starsite.finance.repository.FinanceImportLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 账单导入记录仓储实现：调用 dal 的 {@link FinanceImportLogMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class FinanceImportLogRepositoryImpl implements FinanceImportLogRepository {

    private final FinanceImportLogMapper importLogMapper;

    @Override
    public FinanceImportLog save(FinanceImportLog importLog) {
        FinanceImportLogDO row = FinanceImportLogConverter.toDO(importLog);
        importLogMapper.insert(row);
        importLog.setId(row.getId());
        return importLog;
    }

    @Override
    public List<FinanceImportLog> listByOwner(Long ownerMemberId, Long fileId) {
        return importLogMapper.selectByOwner(ownerMemberId, fileId)
                .stream().map(FinanceImportLogConverter::toEntity).toList();
    }
}
