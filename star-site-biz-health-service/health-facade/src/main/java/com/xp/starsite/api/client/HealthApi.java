package com.xp.starsite.api.client;

import com.xp.starsite.api.dto.health.HealthReportDTO;
import com.xp.starsite.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * health 服务内部接口（Feign 契约）：health 作为后续垂直健康产品（复查闭环、儿童生长、
 * 用药管理等）的横向数据服务，经本契约只读暴露报告与指标。
 * 服务端实现见 health 服务 HealthInternalController。
 *
 * /internal/** 不经网关路由（外网 404），仅限内网服务间调用。
 * 归属校验：health 侧按 memberId 校验报告归属（fail-closed），消费方仍须自行
 * 经 FamilyOwnershipGuard 校验成员归属后再调用。
 *
 * @author xiaopu
 * @since 2026-10
 */
@FeignClient(name = "star-site-biz-health", contextId = "healthInternalApi", path = "/internal/health")
public interface HealthApi {

    /**
     * 报告详情（含全部指标，按 sortNo 排序）
     *
     * @param memberId 当前登录账号（归属校验主体）
     * @param reportId 报告 id
     * @return 报告快照（含指标全集）；不存在时 code=400
     */
    @GetMapping("/reports/{reportId}")
    R<HealthReportDTO> getReportDetail(@RequestParam("memberId") Long memberId,
                                       @PathVariable("reportId") Long reportId);
}
