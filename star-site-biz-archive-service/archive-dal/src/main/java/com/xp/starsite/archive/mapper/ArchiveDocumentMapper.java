package com.xp.starsite.archive.mapper;

import com.xp.starsite.archive.entity.ArchiveDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 家庭证件数据访问（DAL）：SQL 全部收敛在 mapper XML，业务层只见业务语义方法。
 * 逻辑删：archive_document.deleted，查询一律带 deleted=0，删除为 UPDATE 置位。
 * 主键为应用侧雪花 id——INSERT 显式含 id 列（事务消息载荷需 insert 前拿到 id）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface ArchiveDocumentMapper {

    /** 新建证件（id 雪花，应用侧已赋值；created_at/updated_at 由 DB 默认值维护） */
    int insert(ArchiveDocument document);

    /** 按 id 取证件（含逻辑删过滤，不存在返回 null） */
    ArchiveDocument selectById(@Param("id") Long id);

    /** 显式更新证件全量业务列（updated_at 由 DB 维护） */
    int update(ArchiveDocument document);

    /** 逻辑删：deleted 置 1 */
    int markDeleted(@Param("id") Long id);

    /** 证件分页计数：归属账号 + 可选持有人/类型（与 selectPage 同条件） */
    long countByOwner(@Param("ownerMemberId") Long ownerMemberId,
                      @Param("holderMemberId") Long holderMemberId,
                      @Param("docType") String docType);

    /** 证件分页：按有效期升序（长期/null 靠前，与原实现同序） */
    List<ArchiveDocument> selectPage(@Param("ownerMemberId") Long ownerMemberId,
                                     @Param("holderMemberId") Long holderMemberId,
                                     @Param("docType") String docType,
                                     @Param("offset") long offset,
                                     @Param("limit") int limit);
}
