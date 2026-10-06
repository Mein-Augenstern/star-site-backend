package com.xp.starsite.file.service.impl;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.file.entity.FileRecord;
import com.xp.starsite.file.mapper.FileRecordMapper;
import com.xp.starsite.file.service.FileService;
import com.xp.starsite.file.storage.FileStorage;
import com.xp.starsite.file.vo.FileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

/**
 * 文件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_EXTS = Set.of(".pdf", ".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> ALLOWED_BIZ = Set.of("avatar", "report", "other");
    private static final long MAX_SIZE = 20L * 1024 * 1024;

    private final FileRecordMapper fileRecordMapper;
    private final FileStorage fileStorage;

    @Override
    public FileVO upload(MultipartFile file, String bizType, Long uploaderMemberId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "请选择要上传的文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "文件不能超过 20MB");
        }
        String original = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
        String lower = original.toLowerCase();
        if (ALLOWED_EXTS.stream().noneMatch(lower::endsWith)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "只支持 pdf/jpg/png/webp 格式");
        }
        if (StringUtils.hasText(bizType) && !ALLOWED_BIZ.contains(bizType)) {
            bizType = "other";
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
        record.setBizType(StringUtils.hasText(bizType) ? bizType : "other");
        record.setUploaderMemberId(uploaderMemberId);
        fileRecordMapper.insert(record);
        log.info("文件上传: id={}, name={}, size={}, biz={}", record.getId(), original, file.getSize(), record.getBizType());
        return FileVO.builder()
                .fileId(record.getId())
                .name(original)
                .contentType(record.getContentType())
                .size(record.getSizeBytes())
                .build();
    }

    @Override
    public FileRecord requireFile(Long id) {
        FileRecord record = fileRecordMapper.selectById(id);
        if (record == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "文件不存在");
        }
        return record;
    }
}
