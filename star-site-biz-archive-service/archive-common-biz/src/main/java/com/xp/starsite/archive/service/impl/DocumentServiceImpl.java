package com.xp.starsite.archive.service.impl;

import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.api.dto.FamilyMemberSnapshotDTO;
import com.xp.starsite.api.event.reminder.ReminderCancelEvent;
import com.xp.starsite.api.event.reminder.ReminderRegisterEvent;
import com.xp.starsite.api.event.reminder.ReminderRepeatType;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.mq.EventPublisher;
import com.xp.starsite.common.mq.event.EventTypes;
import com.xp.starsite.common.mq.event.Topics;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.common.util.IdGenerator;
import com.xp.starsite.archive.dto.DocumentSaveDTO;
import com.xp.starsite.archive.entity.ArchiveDocument;
import com.xp.starsite.archive.repository.ArchiveDocumentRepository;
import com.xp.starsite.archive.service.DocumentService;
import com.xp.starsite.archive.vo.DocumentVO;
import com.xp.starsite.archive.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import com.xp.starsite.archive.constant.ArchiveBizType;
import com.xp.starsite.archive.constant.ArchiveReminderBizType;
import org.springframework.transaction.annotation.Transactional;

/**
 * 证件夹实现。提醒事件取舍与保单域一致：
 * 到期提醒（主提醒）走事务消息与落库原子；撤销走普通发送（丢失仅多一条提醒噪声）。
 * <p>
 * 数据访问经仓储层（{@link ArchiveDocumentRepository}，实现位于 core-service、底层调用 dal DAO）；
 * 逻辑删（deleted=0 过滤 / UPDATE 置位）。
 * 主键为应用侧雪花 id（{@link IdGenerator}）——半消息载荷需在 insert 前拿到 id，
 * 该语义保持不变。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final Set<String> DOC_TYPES = Set.of(
            "id_card", "hukou", "property_cert", "vehicle_license", "diploma",
            "marriage", "birth", "passport", "contract", "other");

    private static final int EXPIRY_ADVANCE_DAYS = 30;

    private final ArchiveDocumentRepository documentRepository;
    private final FamilyOwnershipGuard ownershipGuard;
    private final EventPublisher eventPublisher;
    /** 雪花 id（替代 MyBatis-Plus ASSIGN_ID）：事务消息载荷需 insert 前拿到 id */
    private final IdGenerator idGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO create(Long memberId, DocumentSaveDTO dto) {
        FamilyMemberSnapshotDTO holder = ownershipGuard.requireOwned(memberId, dto.getHolderMemberId());
        ArchiveDocument doc = new ArchiveDocument();
        // 雪花 id 应用侧先赋值：半消息载荷需要 bizId
        doc.setId(idGenerator.nextId());
        applyDto(doc, dto);
        doc.setFamilyId(holder.getFamilyId());
        doc.setOwnerMemberId(memberId);
        doc.setHolderMemberName(holder.getName());
        eventPublisher.publishTransactional(Topics.TOPIC_REMINDER, EventTypes.REMINDER_REGISTER,
                "document", doc.getId(), expiryReminderOf(doc), () -> documentRepository.save(doc));
        log.info("创建证件: id={}, holder={}, type={}", doc.getId(), holder.getName(), doc.getDocType());
        return toVO(documentRepository.findById(doc.getId()).orElse(null));
    }

    @Override
    public PageVO<DocumentVO> list(Long memberId, Long holderMemberId, String docType, long page, long size) {
        if (holderMemberId != null) {
            ownershipGuard.requireOwned(memberId, holderMemberId);
        }
        // 分页 count 先行：0 直接短路，不再取页
        long total = documentRepository.countByOwner(memberId, holderMemberId,
                StringUtils.hasText(docType) ? docType : null);
        if (total == 0) {
            return PageVO.of(List.of(), 0, page, size);
        }
        long offset = (page - 1) * size;
        List<DocumentVO> vos = documentRepository
                .pageByOwner(memberId, holderMemberId, StringUtils.hasText(docType) ? docType : null, offset, (int) size)
                .stream().map(this::toVO).toList();
        return PageVO.of(vos, total, page, size);
    }

    @Override
    public DocumentVO getDetail(Long memberId, Long id) {
        return toVO(requireOwnedDocument(memberId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO update(Long memberId, Long id, DocumentSaveDTO dto) {
        ArchiveDocument doc = requireOwnedDocument(memberId, id);
        FamilyMemberSnapshotDTO holder = ownershipGuard.requireOwned(memberId, dto.getHolderMemberId());
        applyDto(doc, dto);
        doc.setHolderMemberName(holder.getName());
        documentRepository.update(doc);
        // 有效期仍在 → upsert 规则；清空（改长期）→ 撤销
        if (doc.getExpiryDate() != null) {
            eventPublisher.publish(Topics.TOPIC_REMINDER, EventTypes.REMINDER_REGISTER,
                    "document", doc.getId(), expiryReminderOf(doc));
        } else {
            publishCancel(doc);
        }
        return toVO(documentRepository.findById(id).orElse(null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long memberId, Long id) {
        ArchiveDocument doc = requireOwnedDocument(memberId, id);
        documentRepository.markDeleted(id);
        publishCancel(doc);
        log.info("删除证件: id={}", id);
    }

    private ArchiveDocument requireOwnedDocument(Long memberId, Long id) {
        ArchiveDocument doc = documentRepository.findById(id).orElse(null);
        if (doc == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "证件不存在");
        }
        if (!doc.getOwnerMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return doc;
    }

    /** 到期提醒：有效期前 30 天单次提醒 */
    private ReminderRegisterEvent expiryReminderOf(ArchiveDocument doc) {
        ReminderRegisterEvent event = new ReminderRegisterEvent();
        event.setSource(ArchiveReminderBizType.SOURCE);
        event.setBizType(ArchiveReminderBizType.DOCUMENT_EXPIRY);
        event.setBizId(doc.getId());
        event.setFamilyId(doc.getFamilyId());
        event.setOwnerMemberId(doc.getOwnerMemberId());
        event.setTitle(doc.getHolderMemberName() + " " + docTypeLabel(doc.getDocType()) + " 到期");
        event.setFirstRemindAt(doc.getExpiryDate() == null ? null : doc.getExpiryDate().minusDays(EXPIRY_ADVANCE_DAYS));
        event.setRepeatType(ReminderRepeatType.NONE);
        event.setAdvanceDays(EXPIRY_ADVANCE_DAYS);
        return event;
    }

    private void publishCancel(ArchiveDocument doc) {
        ReminderCancelEvent event = new ReminderCancelEvent();
        event.setSource(ArchiveReminderBizType.SOURCE);
        event.setBizType(ArchiveReminderBizType.DOCUMENT_EXPIRY);
        event.setBizId(doc.getId());
        eventPublisher.publish(Topics.TOPIC_REMINDER, EventTypes.REMINDER_CANCEL, ArchiveBizType.DOCUMENT, doc.getId(), event);
    }

    private void applyDto(ArchiveDocument doc, DocumentSaveDTO dto) {
        if (!DOC_TYPES.contains(dto.getDocType())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "不支持的证件类型");
        }
        doc.setDocType(dto.getDocType());
        doc.setHolderMemberId(dto.getHolderMemberId());
        doc.setDocNo(dto.getDocNo());
        doc.setIssueDate(dto.getIssueDate());
        doc.setExpiryDate(dto.getExpiryDate());
        doc.setFileId(dto.getFileId());
        doc.setRemark(dto.getRemark());
    }

    private String docTypeLabel(String docType) {
        return switch (docType) {
            case "id_card" -> "身份证";
            case "hukou" -> "户口本";
            case "property_cert" -> "房产证";
            case "vehicle_license" -> "行驶证";
            case "diploma" -> "学历证书";
            case "marriage" -> "结婚证";
            case "birth" -> "出生证明";
            case "passport" -> "护照";
            case "contract" -> "合同";
            default -> "证件";
        };
    }

    private DocumentVO toVO(ArchiveDocument doc) {
        DocumentVO vo = new DocumentVO();
        BeanUtils.copyProperties(doc, vo);
        return vo;
    }
}
