package com.xp.starsite.file.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.file.controller.actions.FileActions;
import com.xp.starsite.file.entity.FileRecord;
import com.xp.starsite.file.service.FileService;
import com.xp.starsite.file.storage.FileStorage;
import com.xp.starsite.file.vo.FileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
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
 * 业务逻辑统一经模板包裹（动作名 file.*）；
 * 例外：view 返回 {@link ResponseEntity} 文件流而非 {@link R}，模板不能包它（见方法注释）。
 *
 * @author muyi
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

    /**
     * 上传文件（成功后发 FileUploadedEvent；bizType 决定下游是否消费）
     *
     * @param file     上传的文件（multipart 字段名 file）
     * @param bizType  可选，业务类型（决定下游是否消费事件）
     * @param metadata 全部表单参数随事件透传（如 accountId）
     * @param memberId 当前登录账号（JWT 注入，作为上传者）
     * @return 文件视图（含文件 id 与访问信息）
     */
    @PostMapping("/upload")
    public R<FileVO> upload(@RequestParam("file") MultipartFile file,
                            @RequestParam(value = "bizType", required = false) String bizType,
                            @RequestParam java.util.Map<String, String> metadata, // 全部表单参数随事件透传（如 accountId）
                            @RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return writeTemplate.write(FileActions.UPLOAD,
                () -> fileService.upload(file, bizType, memberId, metadata));
    }

    /**
     * 在线预览文件流；越权防线（仅上传者本人可读）下沉在 {@link FileService#requireOwnedFile}
     * （不存在抛 BizException、越权抛 FORBIDDEN，语义由全局异常处理器兜底）。
     * <p>内容类型解析选择下沉 service：媒体类型是文件域的业务属性（记录 contentType 的规范化），
     * 且控制器不应感知解析规则；本方法只把 service 返回的 MIME 字符串写入响应头（HTTP 表达职责），
     * 与原「解析为媒体类型再设置 Content-Type」的线上行为逐字节等价。
     * <p>不套模板：本方法返回 {@code ResponseEntity<InputStreamResource>} 文件流而非 {@link R}，
     * 模板的响应封装（成功 R.ok / 失败 R.fail）不能包它。
     *
     * @param memberId 当前登录账号（JWT 注入，用于越权校验）
     * @param id       文件 id
     * @return 文件流响应（inline；Content-Type 取记录 contentType 规范化值，缺省 octet-stream）
     */
    @GetMapping("/{id}/view")
    public ResponseEntity<InputStreamResource> view(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                                    @PathVariable Long id) {
        FileRecord record = fileService.requireOwnedFile(memberId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, fileService.resolveContentType(record.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + record.getFileName() + "\"")
                .body(new InputStreamResource(fileStorage.open(record.getStorageKey())));
    }
}

/**
 * 文件服务间内部接口（Feign 提供方）。/internal/** 不经网关，外网 404。
 * <p>
 * 业务逻辑统一经查询模板包裹：模板包掉成功与失败的全部响应处理，
 * Controller 一行返回；动作命名规范 file.internal.*。
 *
 * @author muyi
 * @since 2026-10
 */
@RestController
@RequestMapping("/internal/file")
@RequiredArgsConstructor
class FileInternalController {

    private final FileService fileService;
    private final FileStorage fileStorage;
    private final QueryTemplate queryTemplate;

    /**
     * 服务间取文件内容（如账单导入解析）
     *
     * @param id 文件 id
     * @return 文件字节内容
     */
    @GetMapping("/{id}/content")
    public R<byte[]> content(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return queryTemplate.query(FileActions.INTERNAL_CONTENT, () -> {
            FileRecord record = fileService.requireFile(id);
            try {
                return fileStorage.open(record.getStorageKey()).readAllBytes();
            } catch (java.io.IOException e) {
                throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "文件读取失败");
            }
        });
    }
}
