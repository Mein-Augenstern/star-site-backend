package com.xp.starsite.finance.converter;

import com.xp.starsite.finance.dal.dataobject.FinanceAccountDO;
import com.xp.starsite.finance.entity.FinanceAccount;
import org.springframework.beans.BeanUtils;

/**
 * 资金账户模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class FinanceAccountConverter {

    private FinanceAccountConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static FinanceAccountDO toDO(FinanceAccount account) {
        FinanceAccountDO row = new FinanceAccountDO();
        row.setId(account.getId());
        row.setFamilyId(account.getFamilyId());
        row.setOwnerMemberId(account.getOwnerMemberId());
        row.setName(account.getName());
        row.setAccountType(account.getAccountType());
        row.setBalance(account.getBalance());
        row.setInitialBalance(account.getInitialBalance());
        row.setLiabilityFlag(account.getLiabilityFlag());
        row.setInstitution(account.getInstitution());
        row.setRemark(account.getRemark());
        row.setDeleted(account.getDeleted());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static FinanceAccount toEntity(FinanceAccountDO row) {
        FinanceAccount account = new FinanceAccount();
        BeanUtils.copyProperties(row, account);
        return account;
    }
}
