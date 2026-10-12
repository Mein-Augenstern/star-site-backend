package com.xp.starsite.health.converter;

import com.xp.starsite.health.dal.dataobject.DailyMetricDO;
import com.xp.starsite.health.entity.DailyMetric;
import org.springframework.beans.BeanUtils;

/**
 * 日常健康指标模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class MetricConverter {

    private MetricConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static DailyMetricDO toDO(DailyMetric metric) {
        DailyMetricDO row = new DailyMetricDO();
        row.setId(metric.getId());
        row.setFamilyMemberId(metric.getFamilyMemberId());
        row.setMetricDate(metric.getMetricDate());
        row.setMetricType(metric.getMetricType());
        row.setValueNum(metric.getValueNum());
        row.setRemark(metric.getRemark());
        row.setDeleted(metric.getDeleted());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static DailyMetric toEntity(DailyMetricDO row) {
        DailyMetric metric = new DailyMetric();
        BeanUtils.copyProperties(row, metric);
        return metric;
    }
}
