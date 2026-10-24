package com.xp.starsite.health.mapper;

import com.xp.starsite.health.dal.dataobject.HealthReportDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 体检报告数据访问（DAL）：SQL 全部收敛在 mapper XML，业务层只见业务语义方法。
 * 存储引擎可替换（MySQL/Lindorm/TBase）——接口不变，换 XML/实现即可。
 * 逻辑删：health_report.deleted，查询一律带 deleted=0，删除为 UPDATE 置位。
 *
 * @author muyi
 * @since 2026-10
 */
@Mapper
public interface HealthReportMapper {

    /** 新建报告（业务列插入；created_at/updated_at 由 DB 默认值维护） */
    int insert(HealthReportDO report);

    /** 按 id 取报告（含逻辑删过滤，不存在返回 null） */
    HealthReportDO selectById(@Param("id") Long id);

    /** 逻辑删：deleted 置 1 */
    int markDeleted(@Param("id") Long id);

    /** 报告分页：限定成员范围（必填）+ 可选年份，按体检日期倒序 */
    List<HealthReportDO> selectPage(@Param("scopeIds") Collection<Long> scopeIds,
                                  @Param("year") Integer year,
                                  @Param("offset") long offset,
                                  @Param("limit") int limit);

    /** 报告分页计数（与 selectPage 同条件） */
    long countByScope(@Param("scopeIds") Collection<Long> scopeIds,
                      @Param("year") Integer year);

    /**
     * 关键词搜索：命中标题/医院/总结，或 id 命中指标名命中的报告集合；最多 50 条
     *
     * @param reportIds 指标名命中的报告 id 集合（可为空集合，此时不按指标命中）
     */
    List<HealthReportDO> search(@Param("scopeIds") Collection<Long> scopeIds,
                              @Param("keyword") String keyword,
                              @Param("reportIds") Collection<Long> reportIds);

    /** AI 解读结果回填（解读文本 + 解读时间；异步解读完成后调用） */
    int updateInterpretation(@Param("id") Long id,
                             @Param("interpretation") String interpretation,
                             @Param("interpretedAt") LocalDateTime interpretedAt);
}
