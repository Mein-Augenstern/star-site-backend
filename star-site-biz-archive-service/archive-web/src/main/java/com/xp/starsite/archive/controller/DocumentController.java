package com.xp.starsite.archive.controller;

import com.xp.starsite.archive.dto.DocumentSaveDTO;
import com.xp.starsite.archive.service.DocumentService;
import com.xp.starsite.archive.vo.DocumentVO;
import com.xp.starsite.archive.vo.PageVO;
import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
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
 * <p>
 * 业务逻辑统一经模板包裹：模板包掉成功与失败的全部响应处理（成功 R.ok / 失败 R.fail），
 * Controller 一行返回；动作命名规范 archive.document.*。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/archive/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 创建证件（有效期非空 → 发到期提醒事件）
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param dto      证件元数据
     * @return 新建证件视图
     */
    @PostMapping
    public R<DocumentVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                @Valid @RequestBody DocumentSaveDTO dto) {
        return writeTemplate.write("archive.document.create",
                () -> documentService.create(memberId, dto));
    }

    /**
     * 证件分页列表（筛选：持有人、类型）
     *
     * @param memberId       当前登录账号（JWT 注入，用于归属校验）
     * @param holderMemberId 可选，按持有人成员过滤
     * @param docType        可选，按证件类型过滤
     * @param page           页码（从 1 起）
     * @param size           每页条数
     * @return 证件分页
     */
    @GetMapping
    public R<PageVO<DocumentVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                      @RequestParam(required = false) Long holderMemberId,
                                      @RequestParam(required = false) String docType,
                                      @RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "20") long size) {
        return queryTemplate.query("archive.document.page",
                () -> documentService.list(memberId, holderMemberId, docType, page, size));
    }

    /**
     * 证件详情
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param id       证件 id
     * @return 证件视图
     */
    @GetMapping("/{id}")
    public R<DocumentVO> detail(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                @PathVariable Long id) {
        return queryTemplate.query("archive.document.detail",
                () -> documentService.getDetail(memberId, id));
    }

    /**
     * 更新证件（有效期变更重发/撤销提醒）
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param id       证件 id
     * @param dto      证件元数据（全量覆盖）
     * @return 更新后证件视图
     */
    @PutMapping("/{id}")
    public R<DocumentVO> update(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                @PathVariable Long id,
                                @Valid @RequestBody DocumentSaveDTO dto) {
        return writeTemplate.write("archive.document.update",
                () -> documentService.update(memberId, id, dto));
    }

    /**
     * 删除证件（逻辑删；撤销到期提醒）
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param id       证件 id
     * @return 统一成功响应（无数据载荷）
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        return writeTemplate.write("archive.document.delete",
                () -> documentService.delete(memberId, id));
    }
}
