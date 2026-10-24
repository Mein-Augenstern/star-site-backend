package com.xp.starsite.file.mapper;

import com.xp.starsite.file.dal.dataobject.FileRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * file 文件记录表数据访问（DAL）：SQL 全部收敛在 mapper XML，业务层只见业务语义方法。
 * 主键自增：INSERT 不含 id（useGeneratedKeys 回填），created_at 由 DB 默认值维护。
 * 表无 deleted 列（文件不做逻辑删），查询不过滤逻辑删。
 *
 * @author muyi
 * @since 2026-10
 */
@Mapper
public interface FileRecordMapper {

    /** 新建文件记录 */
    int insert(FileRecordDO record);

    /** 按 id 取记录（不存在返回 null） */
    FileRecordDO selectById(@Param("id") Long id);
}
