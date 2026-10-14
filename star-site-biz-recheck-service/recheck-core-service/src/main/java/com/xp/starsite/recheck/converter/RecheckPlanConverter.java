package com.xp.starsite.recheck.converter;

import com.xp.starsite.recheck.dal.dataobject.RecheckPlanDO;
import com.xp.starsite.recheck.entity.RecheckPlan;
import org.springframework.beans.BeanUtils;

/**
 * 复查计划模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class RecheckPlanConverter {

    private RecheckPlanConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static RecheckPlanDO toDO(RecheckPlan plan) {
        RecheckPlanDO row = new RecheckPlanDO();
        BeanUtils.copyProperties(plan, row);
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static RecheckPlan toEntity(RecheckPlanDO row) {
        RecheckPlan plan = new RecheckPlan();
        BeanUtils.copyProperties(row, plan);
        return plan;
    }
}
