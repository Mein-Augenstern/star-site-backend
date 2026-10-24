package com.xp.starsite.finance.mapper;

import com.xp.starsite.finance.dal.dataobject.FinanceImportLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 账单导入记录数据访问（DAL）：finance_import_log，自增主键（DB 生成），无逻辑删。
 *
 * @author muyi
 * @since 2026-10
 */
@Mapper
public interface FinanceImportLogMapper {

    /** 插入导入记录（id 由 DB 自增，useGeneratedKeys 回填） */
    int insert(FinanceImportLogDO importLog);

    /** 导入记录列表：限定 owner + 可选文件，按 id 倒序（前端轮询导入进度） */
    List<FinanceImportLogDO> selectByOwner(@Param("ownerMemberId") Long ownerMemberId,
                                         @Param("fileId") Long fileId);
}
