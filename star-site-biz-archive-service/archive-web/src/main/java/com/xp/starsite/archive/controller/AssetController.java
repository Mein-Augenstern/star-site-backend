package com.xp.starsite.archive.controller;

import com.xp.starsite.archive.dto.AssetSaveDTO;
import com.xp.starsite.archive.service.AssetService;
import com.xp.starsite.archive.vo.AssetSummaryVO;
import com.xp.starsite.archive.vo.AssetVO;
import com.xp.starsite.archive.vo.PageVO;
import com.xp.starsite.archive.vo.ValuationPointVO;
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

import java.util.List;

/**
 * 家庭资产接口（全部需 JWT；memberId 用于归属校验）
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/archive/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    public R<AssetVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                             @Valid @RequestBody AssetSaveDTO dto) {
        return R.ok(assetService.create(memberId, dto));
    }

    @GetMapping
    public R<PageVO<AssetVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                   @RequestParam(required = false) String assetType,
                                   @RequestParam(defaultValue = "1") long page,
                                   @RequestParam(defaultValue = "20") long size) {
        return R.ok(assetService.list(memberId, assetType, page, size));
    }

    /** 资产总览（注意路由顺序：先声明固定路径再声明 {id}） */
    @GetMapping("/summary")
    public R<AssetSummaryVO> summary(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(assetService.summary(memberId));
    }

    @GetMapping("/{id}")
    public R<AssetVO> detail(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                             @PathVariable Long id) {
        return R.ok(assetService.getDetail(memberId, id));
    }

    @GetMapping("/{id}/valuations")
    public R<List<ValuationPointVO>> valuations(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                                @PathVariable Long id) {
        return R.ok(assetService.valuations(memberId, id));
    }

    @PutMapping("/{id}")
    public R<AssetVO> update(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                             @PathVariable Long id,
                             @Valid @RequestBody AssetSaveDTO dto) {
        return R.ok(assetService.update(memberId, id, dto));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        assetService.delete(memberId, id);
        return R.ok();
    }
}
