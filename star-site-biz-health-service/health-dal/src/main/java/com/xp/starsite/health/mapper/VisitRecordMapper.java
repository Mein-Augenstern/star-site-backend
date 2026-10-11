package com.xp.starsite.health.mapper;

import com.xp.starsite.health.entity.VisitRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 就医记录数据访问（DAL）：visit_record，逻辑删。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface VisitRecordMapper {

    /** 新建就医记录 */
    int insert(VisitRecord visit);

    /** 按 id 取（含逻辑删过滤） */
    VisitRecord selectById(@Param("id") Long id);

    /** 逻辑删 */
    int markDeleted(@Param("id") Long id);

    /** 分页：限定成员范围，按就诊日期倒序 */
    List<VisitRecord> selectPage(@Param("scopeIds") Collection<Long> scopeIds,
                                 @Param("offset") long offset,
                                 @Param("limit") int limit);

    /** 分页计数（与 selectPage 同条件） */
    long countByScope(@Param("scopeIds") Collection<Long> scopeIds);
}
