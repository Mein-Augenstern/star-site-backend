package com.xp.starsite.archive.repository.impl;

import com.xp.starsite.archive.converter.ArchiveDocumentConverter;
import com.xp.starsite.archive.dal.dataobject.ArchiveDocumentDO;
import com.xp.starsite.archive.entity.ArchiveDocument;
import com.xp.starsite.archive.mapper.ArchiveDocumentMapper;
import com.xp.starsite.archive.repository.ArchiveDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 家庭证件仓储实现：调用 dal 的 {@link ArchiveDocumentMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class ArchiveDocumentRepositoryImpl implements ArchiveDocumentRepository {

    private final ArchiveDocumentMapper documentMapper;

    @Override
    public ArchiveDocument save(ArchiveDocument document) {
        ArchiveDocumentDO row = ArchiveDocumentConverter.toDO(document);
        documentMapper.insert(row);
        document.setId(row.getId());
        return document;
    }

    @Override
    public Optional<ArchiveDocument> findById(Long id) {
        return Optional.ofNullable(documentMapper.selectById(id)).map(ArchiveDocumentConverter::toEntity);
    }

    @Override
    public void update(ArchiveDocument document) {
        documentMapper.update(ArchiveDocumentConverter.toDO(document));
    }

    @Override
    public void markDeleted(Long id) {
        documentMapper.markDeleted(id);
    }

    @Override
    public long countByOwner(Long ownerMemberId, Long holderMemberId, String docType) {
        return documentMapper.countByOwner(ownerMemberId, holderMemberId, docType);
    }

    @Override
    public List<ArchiveDocument> pageByOwner(Long ownerMemberId, Long holderMemberId, String docType,
                                             long offset, int limit) {
        return documentMapper.selectPage(ownerMemberId, holderMemberId, docType, offset, limit)
                .stream().map(ArchiveDocumentConverter::toEntity).toList();
    }
}
