package com.xp.starsite.archive.repository;

import com.xp.starsite.archive.entity.ArchiveDocument;

import java.util.List;
import java.util.Optional;

/**
 * 家庭证件仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author muyi
 * @since 2026-10
 */
public interface ArchiveDocumentRepository {

    /** 保存（新增；雪花 id 应用侧已赋值） */
    ArchiveDocument save(ArchiveDocument document);

    /** 按 id 取（含逻辑删过滤） */
    Optional<ArchiveDocument> findById(Long id);

    /** 显式更新证件全量业务列 */
    void update(ArchiveDocument document);

    /** 逻辑删 */
    void markDeleted(Long id);

    /** 证件分页计数：归属账号 + 可选持有人/类型 */
    long countByOwner(Long ownerMemberId, Long holderMemberId, String docType);

    /** 证件分页：按有效期升序（长期/null 靠前） */
    List<ArchiveDocument> pageByOwner(Long ownerMemberId, Long holderMemberId, String docType,
                                      long offset, int limit);
}
