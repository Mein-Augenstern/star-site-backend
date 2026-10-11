package com.xp.starsite.health.mapper;

import com.xp.starsite.health.entity.DailyMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 日常指标数据访问（DAL）：daily_metric（血压/血糖/体重等按日自测），逻辑删。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface DailyMetricMapper {

    /** 单条插入（血压一次两条由业务层循环调用） */
    int insert(DailyMetric metric);

    /** 按 id 取（含逻辑删过滤） */
    DailyMetric selectById(@Param("id") Long id);

    /** 逻辑删 */
    int markDeleted(@Param("id") Long id);

    /** 分页：限定成员范围 + 可选指标类型，按测量日期倒序 */
    List<DailyMetric> selectPage(@Param("scopeIds") Collection<Long> scopeIds,
                                 @Param("metricType") String metricType,
                                 @Param("offset") long offset,
                                 @Param("limit") int limit);

    /** 分页计数（与 selectPage 同条件） */
    long countByScope(@Param("scopeIds") Collection<Long> scopeIds,
                      @Param("metricType") String metricType);

    /** 某成员某指标趋势（按测量日期升序，孕期体重曲线等） */
    List<DailyMetric> selectTrend(@Param("familyMemberId") Long familyMemberId,
                                  @Param("metricType") String metricType);
}
