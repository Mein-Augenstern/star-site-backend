package com.xp.starsite.file.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.file.entity.FileRecord;
import com.xp.starsite.file.service.FileService;
import com.xp.starsite.file.storage.FileStorage;
import com.xp.starsite.file.vo.FileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件接口（全部需 JWT；前端预览用 axios blob 取流）
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileStorage fileStorage;

    @PostMapping("/upload")
    public R<FileVO> upload(@RequestParam("file") MultipartFile file,
                            @RequestParam(value = "bizType", required = false) String bizType,
                            @RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(fileService.upload(file, bizType, memberId));
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<InputStreamResource> view(@PathVariable Long id) {
        FileRecord record = fileService.requireFile(id);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (record.getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(record.getContentType());
            } catch (Exception ignored) {
                // 保底 octet-stream
            }
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + record.getFileName() + "\"")
                .body(new InputStreamResource(fileStorage.open(record.getStorageKey())));
    }
}
