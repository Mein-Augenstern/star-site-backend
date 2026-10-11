package com.xp.starsite.health.mapper;

import com.xp.starsite.health.entity.HealthIndicator;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 报告指标数据访问（DAL）：health_indicator 无逻辑删（随报告重录物理清除）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface HealthIndicatorMapper {

    /** 单条指标插入（报告重录时由业务层循环调用） */
    int insert(HealthIndicator indicator);

    /** 清空报告旧指标（重录覆盖语义） */
    int deleteByReportId(@Param("reportId") Long reportId);

    /** 报告详情的指标全集（按 sortNo,id 升序） */
    List<HealthIndicator> selectByReportId(@Param("reportId") Long reportId);

    /** 某成员某指标历年趋势（按报告年份升序） */
    List<HealthIndicator> selectTrend(@Param("familyMemberId") Long familyMemberId,
                                      @Param("itemName") String itemName);

    /** 指标名模糊命中（供报告搜索取报告 id 集合） */
    List<HealthIndicator> selectByKeywordLike(@Param("keyword") String keyword);
}
