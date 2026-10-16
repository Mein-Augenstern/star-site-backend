package com.xp.starsite.file.service.impl;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.file.entity.FileRecord;
import com.xp.starsite.file.repository.FileRecordRepository;
import com.xp.starsite.file.service.FileService;
import com.xp.starsite.common.mq.EventPublisher;
import com.xp.starsite.common.mq.event.EventTypes;
import com.xp.starsite.common.mq.event.Topics;
import com.xp.starsite.api.event.file.FileUploadedEvent;
import com.xp.starsite.file.storage.FileStorage;
import com.xp.starsite.file.vo.FileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import com.xp.starsite.file.constant.FileBizType;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文件服务实现：校验类型/大小 → 存储落盘（本地或 OSS）→ 建记录 → 发布上传完成事件。
 * 账单导入场景额外放行 CSV；存储实现按 file.storage-type 自动切换。
 * 数据访问经仓储层（{@link FileRecordRepository}，实现位于 core-service、底层调用 dal DAO）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_EXTS = Set.of(".pdf", ".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> ALLOWED_BIZ = Set.of("avatar", "report", "other", "bill_import");
    private static final long MAX_SIZE = 20L * 1024 * 1024;

    private final FileRecordRepository fileRecordRepository;
    private final FileStorage fileStorage;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileVO upload(MultipartFile file, String bizType, Long uploaderMemberId,
                         java.util.Map<String, String> metadata) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "请选择要上传的文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "文件不能超过 20MB");
        }
        String original = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
        String lower = original.toLowerCase();
        // 账单导入场景放行 CSV（微信/支付宝导出）
        boolean billImport = "bill_import".equals(bizType);
        if (ALLOWED_EXTS.stream().noneMatch(lower::endsWith)
                && !(billImport && lower.endsWith(".csv"))) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(),
                    billImport ? "账单只支持 CSV 格式" : "只支持 pdf/jpg/png/webp 格式");
        }
        if (StringUtils.hasText(bizType) && !ALLOWED_BIZ.contains(bizType)) {
            bizType = FileBizType.OTHER;
        }

        String storageKey;
        try {
            storageKey = fileStorage.save(file.getInputStream(), original, file.getContentType());
        } catch (IOException e) {
            log.error("读取上传流失败", e);
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "文件上传失败");
        }

        FileRecord record = new FileRecord();
        record.setFileName(original);
        record.setStorageKey(storageKey);
        record.setContentType(file.getContentType());
        record.setSizeBytes(file.getSize());
        record.setBizType(StringUtils.hasText(bizType) ? bizType : FileBizType.OTHER);
        record.setUploaderMemberId(uploaderMemberId);
        fileRecordRepository.save(record);
        log.info("文件上传: id={}, name={}, size={}, biz={}", record.getId(), original, file.getSize(), record.getBizType());
        // 上传完成事件（普通消息：丢发的后果是消费方漏处理该文件，重传即恢复）
        FileUploadedEvent event = new FileUploadedEvent();
        event.setFileId(record.getId());
        event.setBizType(record.getBizType());
        event.setUploaderMemberId(uploaderMemberId);
        event.setFileName(original);
        event.setAttributes(metadata);
        eventPublisher.publish(Topics.TOPIC_FILE, EventTypes.FILE_UPLOADED,
                record.getBizType(), record.getId(), event);

        return FileVO.builder()
                .fileId(record.getId())
                .name(original)
                .contentType(record.getContentType())
                .size(record.getSizeBytes())
                .build();
    }

    @Override
    public FileRecord requireFile(Long id) {
        FileRecord record = fileRecordRepository.findById(id).orElse(null);
        if (record == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "文件不存在");
        }
        return record;
    }

    @Override
    public FileRecord requireOwnedFile(Long memberId, Long id) {
        FileRecord record = requireFile(id);
        if (!record.getUploaderMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return record;
    }

    @Override
    public String resolveContentType(String contentType) {
        if (contentType == null) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        try {
            // 与 MediaType.parseMediaType(...).toString() 等价：预览响应头取规范化形式
            return MediaType.parseMediaType(contentType).toString();
        } catch (Exception ignored) {
            // 保底 octet-stream
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}
