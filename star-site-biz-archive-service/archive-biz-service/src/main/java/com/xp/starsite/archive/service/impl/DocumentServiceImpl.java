package com.xp.starsite.archive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.xp.starsite.archive.dto.DocumentSaveDTO;
import com.xp.starsite.archive.entity.ArchiveDocument;
import com.xp.starsite.archive.mapper.ArchiveDocumentMapper;
import com.xp.starsite.archive.service.DocumentService;
import com.xp.starsite.archive.vo.DocumentVO;
import com.xp.starsite.archive.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 证件夹实现。提醒事件取舍与保单域一致：
 * 到期提醒（主提醒）走事务消息与落库原子；撤销走普通发送（丢失仅多一条提醒噪声）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final Set<String> DOC_TYPES = Set.of(
            "id_card", "hukou", "property_cert", "vehicle_license", "diploma",
            "marriage", "birth", "passport", "contract", "other");

    private static final int EXPIRY_ADVANCE_DAYS = 30;

    private final ArchiveDocumentMapper documentMapper;
    private final FamilyOwnershipGuard ownershipGuard;
    private final EventPublisher eventPublisher;

    @Override
    public DocumentVO create(Long memberId, DocumentSaveDTO dto) {
        FamilyMemberSnapshotDTO holder = ownershipGuard.requireOwned(memberId, dto.getHolderMemberId());
        ArchiveDocument doc = new ArchiveDocument();
        // 雪花 id 应用侧先赋值：半消息载荷需要 bizId
        doc.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
        applyDto(doc, dto);
        doc.setFamilyId(holder.getFamilyId());
        doc.setOwnerMemberId(memberId);
        doc.setHolderMemberName(holder.getName());
        eventPublisher.publishTransactional(Topics.TOPIC_REMINDER, EventTypes.REMINDER_REGISTER,
                "document", doc.getId(), expiryReminderOf(doc), () -> documentMapper.insert(doc));
        log.info("创建证件: id={}, holder={}, type={}", doc.getId(), holder.getName(), doc.getDocType());
        return toVO(documentMapper.selectById(doc.getId()));
    }

    @Override
    public PageVO<DocumentVO> list(Long memberId, Long holderMemberId, String docType, long page, long size) {
        if (holderMemberId != null) {
            ownershipGuard.requireOwned(memberId, holderMemberId);
        }
        Page<ArchiveDocument> result = documentMapper.selectPage(Page.of(page, size),
                new LambdaQueryWrapper<ArchiveDocument>()
                        .eq(ArchiveDocument::getOwnerMemberId, memberId)
                        .eq(holderMemberId != null, ArchiveDocument::getHolderMemberId, holderMemberId)
                        .eq(StringUtils.hasText(docType), ArchiveDocument::getDocType, docType)
                        .orderByAsc(ArchiveDocument::getExpiryDate));
        return PageVO.of(result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public DocumentVO getDetail(Long memberId, Long id) {
        return toVO(requireOwnedDocument(memberId, id));
    }

    @Override
    public DocumentVO update(Long memberId, Long id, DocumentSaveDTO dto) {
        ArchiveDocument doc = requireOwnedDocument(memberId, id);
        FamilyMemberSnapshotDTO holder = ownershipGuard.requireOwned(memberId, dto.getHolderMemberId());
        applyDto(doc, dto);
        doc.setHolderMemberName(holder.getName());
        documentMapper.updateById(doc);
        // 有效期仍在 → upsert 规则；清空（改长期）→ 撤销
        if (doc.getExpiryDate() != null) {
            eventPublisher.publish(Topics.TOPIC_REMINDER, EventTypes.REMINDER_REGISTER,
                    "document", doc.getId(), expiryReminderOf(doc));
        } else {
            publishCancel(doc);
        }
        return toVO(documentMapper.selectById(id));
    }

    @Override
    public void delete(Long memberId, Long id) {
        ArchiveDocument doc = requireOwnedDocument(memberId, id);
        documentMapper.deleteById(id);
        publishCancel(doc);
        log.info("删除证件: id={}", id);
    }

    private ArchiveDocument requireOwnedDocument(Long memberId, Long id) {
        ArchiveDocument doc = documentMapper.selectById(id);
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
        event.setSource("star-site-archive");
        event.setBizType("document_expiry");
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
        event.setSource("star-site-archive");
        event.setBizType("document_expiry");
        event.setBizId(doc.getId());
        eventPublisher.publish(Topics.TOPIC_REMINDER, EventTypes.REMINDER_CANCEL, "document", doc.getId(), event);
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
