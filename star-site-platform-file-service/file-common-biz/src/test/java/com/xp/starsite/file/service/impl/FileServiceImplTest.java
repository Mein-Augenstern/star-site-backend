package com.xp.starsite.file.service.impl;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.mq.EventPublisher;
import com.xp.starsite.file.entity.FileRecord;
import com.xp.starsite.file.repository.FileRecordRepository;
import com.xp.starsite.file.storage.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * 文件服务单测（FileController 下沉逻辑）：越权防线（仅上传者本人可读）与
 * 内容类型解析回落（mock 全部依赖，不连库、不落盘）。
 *
 * @author muyi
 * @since 2026-10
 */
@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    private static final long ME = 7L;
    private static final long OTHER = 8L;

    @Mock
    private FileRecordRepository fileRecordRepository;
    @Mock
    private FileStorage fileStorage;
    @Mock
    private EventPublisher eventPublisher;

    private FileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FileServiceImpl(fileRecordRepository, fileStorage, eventPublisher);
    }

    private FileRecord record(Long uploaderMemberId, String contentType) {
        FileRecord r = new FileRecord();
        r.setId(1L);
        r.setFileName("账单.csv");
        r.setUploaderMemberId(uploaderMemberId);
        r.setContentType(contentType);
        return r;
    }

    /* ===== requireOwnedFile：越权防线 ===== */

    @Test
    void ownerCanReadRecord() {
        when(fileRecordRepository.findById(1L)).thenReturn(Optional.of(record(ME, "text/csv")));

        FileRecord out = service.requireOwnedFile(ME, 1L);

        assertEquals(ME, out.getUploaderMemberId().longValue());
    }

    @Test
    void otherMembersReaderThrows403() {
        when(fileRecordRepository.findById(1L)).thenReturn(Optional.of(record(ME, "text/csv")));

        BizException e = assertThrows(BizException.class, () -> service.requireOwnedFile(OTHER, 1L));
        assertEquals(403, e.getCode());
    }

    @Test
    void missingFileThrows400() {
        when(fileRecordRepository.findById(99L)).thenReturn(Optional.empty());

        BizException e = assertThrows(BizException.class, () -> service.requireOwnedFile(ME, 99L));
        assertEquals(400, e.getCode());
        assertEquals("文件不存在", e.getMessage());
    }

    /* ===== resolveContentType：解析回落 ===== */

    @Test
    void resolveContentTypeKeepsStoredMime() {
        assertEquals("text/csv", service.resolveContentType("text/csv"));
    }

    @Test
    void resolveContentTypeNormalizesCase() {
        assertEquals("image/png", service.resolveContentType("IMAGE/PNG"));
    }

    @Test
    void resolveContentTypeFallsBackOnNull() {
        assertEquals("application/octet-stream", service.resolveContentType(null));
    }

    @Test
    void resolveContentTypeFallsBackOnIllegalValue() {
        assertEquals("application/octet-stream", service.resolveContentType("not a media type"));
    }
}
