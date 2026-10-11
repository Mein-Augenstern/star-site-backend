package com.xp.starsite.file.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
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
 * 文件接口：上传/在线预览（全部需 JWT；前端预览用 axios blob 取流）。
 * 业务逻辑统一经模板包裹（动作名 file.*）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileStorage fileStorage;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /** 上传文件（成功后发 FileUploadedEvent；bizType 决定下游是否消费） */
    @PostMapping("/upload")
    public R<FileVO> upload(@RequestParam("file") MultipartFile file,
                            @RequestParam(value = "bizType", required = false) String bizType,
                            @RequestParam java.util.Map<String, String> metadata, // 全部表单参数随事件透传（如 accountId）
                            @RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(writeTemplate.execute("file.upload",
                () -> fileService.upload(file, bizType, memberId, metadata)));
    }

    /** 在线预览文件流；越权防线：仅上传者本人可读（多人协作后升级为家庭范围校验） */
    @GetMapping("/{id}/view")
    public ResponseEntity<InputStreamResource> view(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                                    @PathVariable Long id) {
        return queryTemplate.execute("file.view", () -> {
            FileRecord record = fileService.requireFile(id);
            if (!record.getUploaderMemberId().equals(memberId)) {
                throw new BizException(ResultCode.FORBIDDEN);
            }
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
        });
    }
}

@RestController
@RequestMapping("/internal/file")
@RequiredArgsConstructor
class FileInternalController {

    private final FileService fileService;
    private final FileStorage fileStorage;
    private final QueryTemplate queryTemplate;

    /** 服务间取文件内容（如账单导入解析）；/internal/** 不经网关，外网 404 */
    @GetMapping("/{id}/content")
    public R<byte[]> content(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return queryTemplate.execute("file.internal.content", () -> {
            FileRecord record = fileService.requireFile(id);
            try {
                return R.ok(fileStorage.open(record.getStorageKey()).readAllBytes());
            } catch (java.io.IOException e) {
                throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "文件读取失败");
            }
        });
    }
}
