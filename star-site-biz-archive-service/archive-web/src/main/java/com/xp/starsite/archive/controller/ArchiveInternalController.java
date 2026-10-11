package com.xp.starsite.archive.controller;

import com.xp.starsite.api.dto.AssetSummaryDTO;
import com.xp.starsite.common.result.R;
import com.xp.starsite.archive.service.AssetService;
import com.xp.starsite.archive.vo.AssetSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务间内部接口（Feign 提供方）。/internal/** 不在网关路由表里，外网 404。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/internal/archive")
@RequiredArgsConstructor
public class ArchiveInternalController {

    private final AssetService assetService;

    @GetMapping("/assets/summary")
    public R<AssetSummaryDTO> summary(@RequestParam("memberId") Long memberId) {
        AssetSummaryVO vo = assetService.summary(memberId);
        return R.ok(new AssetSummaryDTO(vo.getTotalValue()));
    }
}
