package com.xp.starsite.health.converter;

import com.xp.starsite.health.dal.dataobject.HealthIndicatorDO;
import com.xp.starsite.health.entity.HealthIndicator;
import org.springframework.beans.BeanUtils;

/**
 * 体检指标模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author muyi
 * @since 2026-10
 */
public final class IndicatorConverter {

    private IndicatorConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static HealthIndicatorDO toDO(HealthIndicator indicator) {
        HealthIndicatorDO row = new HealthIndicatorDO();
        row.setId(indicator.getId());
        row.setReportId(indicator.getReportId());
        row.setFamilyMemberId(indicator.getFamilyMemberId());
        row.setReportYear(indicator.getReportYear());
        row.setItemName(indicator.getItemName());
        row.setItemGroup(indicator.getItemGroup());
        row.setValueNum(indicator.getValueNum());
        row.setValueText(indicator.getValueText());
        row.setUnit(indicator.getUnit());
        row.setRefLow(indicator.getRefLow());
        row.setRefHigh(indicator.getRefHigh());
        row.setAbnormalFlag(indicator.getAbnormalFlag());
        row.setSortNo(indicator.getSortNo());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static HealthIndicator toEntity(HealthIndicatorDO row) {
        HealthIndicator indicator = new HealthIndicator();
        BeanUtils.copyProperties(row, indicator);
        return indicator;
    }
}
