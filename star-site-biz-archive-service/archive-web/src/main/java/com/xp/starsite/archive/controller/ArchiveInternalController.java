package com.xp.starsite.archive.controller;

import com.xp.starsite.api.dto.AssetSummaryDTO;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.archive.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务间内部接口（Feign 提供方）。/internal/** 不在网关路由表里，外网 404。
 * <p>
 * 业务逻辑统一经查询模板包裹：模板包掉成功与失败的全部响应处理（成功 R.ok / 失败 R.fail），
 * Controller 一行返回；动作命名规范 archive.internal.*。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/internal/archive")
@RequiredArgsConstructor
public class ArchiveInternalController {

    private final AssetService assetService;
    private final QueryTemplate queryTemplate;

    /**
     * 资产总览（总估值；供家庭账单等下游服务间调用）
     *
     * @param memberId 家庭顶梁柱账号 id
     * @return 资产总览 DTO（总估值）
     */
    @GetMapping("/assets/summary")
    public R<AssetSummaryDTO> summary(@RequestParam("memberId") Long memberId) {
        return queryTemplate.query("archive.internal.summary",
                () -> new AssetSummaryDTO(assetService.summary(memberId).getTotalValue()));
    }
}
