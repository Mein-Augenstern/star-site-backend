package com.xp.starsite.finance.converter;

import com.xp.starsite.finance.dal.dataobject.FinanceGoalDO;
import com.xp.starsite.finance.entity.FinanceGoal;
import org.springframework.beans.BeanUtils;

/**
 * 家庭目标模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class FinanceGoalConverter {

    private FinanceGoalConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static FinanceGoalDO toDO(FinanceGoal goal) {
        FinanceGoalDO row = new FinanceGoalDO();
        row.setId(goal.getId());
        row.setFamilyId(goal.getFamilyId());
        row.setOwnerMemberId(goal.getOwnerMemberId());
        row.setName(goal.getName());
        row.setTargetAmount(goal.getTargetAmount());
        row.setCurrentAmount(goal.getCurrentAmount());
        row.setDeadline(goal.getDeadline());
        row.setRemark(goal.getRemark());
        row.setDeleted(goal.getDeleted());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static FinanceGoal toEntity(FinanceGoalDO row) {
        FinanceGoal goal = new FinanceGoal();
        BeanUtils.copyProperties(row, goal);
        return goal;
    }
}
