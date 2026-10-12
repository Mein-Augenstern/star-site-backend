package com.xp.starsite.archive.mapper;

import com.xp.starsite.archive.dal.dataobject.ArchiveAssetDO;
import com.xp.starsite.archive.projection.AssetTypeSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 家庭资产数据访问（DAL）：SQL 全部收敛在 mapper XML，业务层只见业务语义方法。
 * 逻辑删：archive_asset.deleted，查询一律带 deleted=0，删除为 UPDATE 置位。
 * 主键为应用侧雪花 id——INSERT 显式含 id 列（年检提醒事务消息需 insert 前拿到 id）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface ArchiveAssetMapper {

    /** 新建资产（id 雪花，应用侧已赋值；created_at/updated_at 由 DB 默认值维护） */
    int insert(ArchiveAssetDO asset);

    /** 按 id 取资产（含逻辑删过滤，不存在返回 null） */
    ArchiveAssetDO selectById(@Param("id") Long id);

    /** 显式更新资产全量业务列（updated_at 由 DB 维护） */
    int update(ArchiveAssetDO asset);

    /** 逻辑删：deleted 置 1 */
    int markDeleted(@Param("id") Long id);

    /** 资产分页计数：归属账号 + 可选类型（与 selectPage 同条件） */
    long countByOwner(@Param("ownerMemberId") Long ownerMemberId,
                      @Param("assetType") String assetType);

    /** 资产分页：按 id 倒序（最新录入在前，与原实现同序） */
    List<ArchiveAssetDO> selectPage(@Param("ownerMemberId") Long ownerMemberId,
                                  @Param("assetType") String assetType,
                                  @Param("offset") long offset,
                                  @Param("limit") int limit);

    /** 全部资产总估值（NULL 估值按 0 计；财务资产负债表聚合的数据源） */
    BigDecimal selectTotalValue(@Param("ownerMemberId") Long ownerMemberId);

    /** 按资产类型分组汇总（总估值 + 件数；总览饼图数据源） */
    List<AssetTypeSummary> selectTypeSummaries(@Param("ownerMemberId") Long ownerMemberId);
}
