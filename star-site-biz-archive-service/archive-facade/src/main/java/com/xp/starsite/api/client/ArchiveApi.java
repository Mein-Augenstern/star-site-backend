package com.xp.starsite.api.client;

import com.xp.starsite.api.dto.AssetSummaryDTO;
import com.xp.starsite.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * archive 服务内部接口（Feign 契约）：资产总览供财务资产负债表聚合。
 * /internal/** 不经网关路由（外网 404），仅限内网服务间调用。
 *
 * @author muyi
 * @since 2026-10
 */
@FeignClient(name = "star-site-biz-archive", contextId = "archiveInternalApi", path = "/internal/archive")
public interface ArchiveApi {

    /** 当前账号家庭的资产总估值（无家庭/无资产返回 0） */
    @GetMapping("/assets/summary")
    R<AssetSummaryDTO> assetSummary(@RequestParam("memberId") Long memberId);
}
