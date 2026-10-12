package com.xp.starsite.archive.converter;

import com.xp.starsite.archive.dal.dataobject.ArchiveDocumentDO;
import com.xp.starsite.archive.entity.ArchiveDocument;
import org.springframework.beans.BeanUtils;

/**
 * 家庭证件模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class ArchiveDocumentConverter {

    private ArchiveDocumentConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static ArchiveDocumentDO toDO(ArchiveDocument document) {
        ArchiveDocumentDO row = new ArchiveDocumentDO();
        row.setId(document.getId());
        row.setFamilyId(document.getFamilyId());
        row.setOwnerMemberId(document.getOwnerMemberId());
        row.setHolderMemberId(document.getHolderMemberId());
        row.setHolderMemberName(document.getHolderMemberName());
        row.setDocType(document.getDocType());
        row.setDocNo(document.getDocNo());
        row.setIssueDate(document.getIssueDate());
        row.setExpiryDate(document.getExpiryDate());
        row.setFileId(document.getFileId());
        row.setRemark(document.getRemark());
        row.setDeleted(document.getDeleted());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static ArchiveDocument toEntity(ArchiveDocumentDO row) {
        ArchiveDocument document = new ArchiveDocument();
        BeanUtils.copyProperties(row, document);
        return document;
    }
}
