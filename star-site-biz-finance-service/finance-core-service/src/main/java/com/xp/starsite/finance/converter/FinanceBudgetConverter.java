package com.xp.starsite.finance.converter;

import com.xp.starsite.finance.dal.dataobject.FinanceBudgetDO;
import com.xp.starsite.finance.entity.FinanceBudget;
import org.springframework.beans.BeanUtils;

/**
 * 月度预算模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author muyi
 * @since 2026-10
 */
public final class FinanceBudgetConverter {

    private FinanceBudgetConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static FinanceBudgetDO toDO(FinanceBudget budget) {
        FinanceBudgetDO row = new FinanceBudgetDO();
        row.setId(budget.getId());
        row.setFamilyId(budget.getFamilyId());
        row.setOwnerMemberId(budget.getOwnerMemberId());
        row.setCategory(budget.getCategory());
        row.setPeriod(budget.getPeriod());
        row.setAmount(budget.getAmount());
        row.setDeleted(budget.getDeleted());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static FinanceBudget toEntity(FinanceBudgetDO row) {
        FinanceBudget budget = new FinanceBudget();
        BeanUtils.copyProperties(row, budget);
        return budget;
    }
}
