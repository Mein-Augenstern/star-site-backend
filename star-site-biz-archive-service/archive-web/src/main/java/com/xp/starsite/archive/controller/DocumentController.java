package com.xp.starsite.archive.controller;

import com.xp.starsite.archive.dto.DocumentSaveDTO;
import com.xp.starsite.archive.service.DocumentService;
import com.xp.starsite.archive.vo.DocumentVO;
import com.xp.starsite.archive.vo.PageVO;
import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 证件夹接口（全部需 JWT；memberId 用于归属校验）
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/archive/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public R<DocumentVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                @Valid @RequestBody DocumentSaveDTO dto) {
        return R.ok(documentService.create(memberId, dto));
    }

    @GetMapping
    public R<PageVO<DocumentVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                      @RequestParam(required = false) Long holderMemberId,
                                      @RequestParam(required = false) String docType,
                                      @RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "20") long size) {
        return R.ok(documentService.list(memberId, holderMemberId, docType, page, size));
    }

    @GetMapping("/{id}")
    public R<DocumentVO> detail(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                @PathVariable Long id) {
        return R.ok(documentService.getDetail(memberId, id));
    }

    @PutMapping("/{id}")
    public R<DocumentVO> update(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                @PathVariable Long id,
                                @Valid @RequestBody DocumentSaveDTO dto) {
        return R.ok(documentService.update(memberId, id, dto));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        documentService.delete(memberId, id);
        return R.ok();
    }
}
