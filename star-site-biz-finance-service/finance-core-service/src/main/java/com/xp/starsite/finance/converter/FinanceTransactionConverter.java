package com.xp.starsite.finance.converter;

import com.xp.starsite.finance.dal.dataobject.FinanceTransactionDO;
import com.xp.starsite.finance.entity.FinanceTransaction;
import org.springframework.beans.BeanUtils;

/**
 * 交易流水模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author muyi
 * @since 2026-10
 */
public final class FinanceTransactionConverter {

    private FinanceTransactionConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static FinanceTransactionDO toDO(FinanceTransaction transaction) {
        FinanceTransactionDO row = new FinanceTransactionDO();
        row.setId(transaction.getId());
        row.setFamilyId(transaction.getFamilyId());
        row.setOwnerMemberId(transaction.getOwnerMemberId());
        row.setAccountId(transaction.getAccountId());
        row.setAccountName(transaction.getAccountName());
        row.setDirection(transaction.getDirection());
        row.setAmount(transaction.getAmount());
        row.setCategory(transaction.getCategory());
        row.setCategoryName(transaction.getCategoryName());
        row.setTransTime(transaction.getTransTime());
        row.setCounterparty(transaction.getCounterparty());
        row.setTransferGroupId(transaction.getTransferGroupId());
        row.setBalanceAfter(transaction.getBalanceAfter());
        row.setSource(transaction.getSource());
        row.setExternalId(transaction.getExternalId());
        row.setFileId(transaction.getFileId());
        row.setRemark(transaction.getRemark());
        row.setDeleted(transaction.getDeleted());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static FinanceTransaction toEntity(FinanceTransactionDO row) {
        FinanceTransaction transaction = new FinanceTransaction();
        BeanUtils.copyProperties(row, transaction);
        return transaction;
    }
}
