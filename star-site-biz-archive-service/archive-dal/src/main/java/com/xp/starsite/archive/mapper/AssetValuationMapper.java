package com.xp.starsite.archive.mapper;

import com.xp.starsite.archive.dal.dataobject.AssetValuationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资产估值历史数据访问（DAL）：SQL 全部收敛在 mapper XML。
 * 主键 DB 自增；只追加不删改（留痕语义），无 deleted 列。
 *
 * @author muyi
 * @since 2026-10
 */
@Mapper
public interface AssetValuationMapper {

    /** 追加一条估值留痕（id DB 自增回填；created_at 由 DB 默认值维护） */
    int insert(AssetValuationDO valuation);

    /** 某资产的估值曲线（按估值时点升序） */
    List<AssetValuationDO> selectByAssetId(@Param("assetId") Long assetId);
}
