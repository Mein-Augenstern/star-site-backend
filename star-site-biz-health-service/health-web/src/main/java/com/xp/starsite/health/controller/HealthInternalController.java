package com.xp.starsite.health.controller;

import com.xp.starsite.api.dto.health.HealthReportDTO;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.health.controller.actions.HealthActions;
import com.xp.starsite.health.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 服务间内部接口（Feign 提供方）：health 横向化的只读契约。
 * health 是后续垂直健康产品（复查闭环、儿童生长、用药管理等）的横向数据服务，
 * 垂直服务经 health-facade 的 HealthApi 调用本控制器，禁止依赖 health 实现。
 * <p>
 * /internal/** 不在网关路由表里，外网请求到网关直接 404；
 * JwtAuthInterceptor 只拦 /api/**，本控制器不经 JWT，仅限内网服务间调用。
 * 归属防线：复用 {@link ReportService} 内部方法的归属校验（首参 memberId，fail-closed），
 * 调用方仍须先经 FamilyOwnershipGuard 校验成员归属。
 * 业务逻辑统一经模板包裹（动作名 health.internal.*）；映射下沉在 service（军规：控制器零映射）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/internal/health")
@RequiredArgsConstructor
public class HealthInternalController {

    private final ReportService reportService;
    private final QueryTemplate queryTemplate;

    /**
     * 报告详情（含全部指标，按 sortNo 排序）
     *
     * @param memberId 当前登录账号（归属校验主体）
     * @param reportId 报告 id
     * @return 报告快照（含指标全集）
     */
    @GetMapping("/reports/{reportId}")
    public R<HealthReportDTO> getReportDetail(@RequestParam("memberId") Long memberId,
                                              @PathVariable("reportId") Long reportId) {
        return queryTemplate.query(HealthActions.INTERNAL_REPORT_DETAIL,
                () -> reportService.getInternalDetail(memberId, reportId));
    }

    /**
     * 报告摘要列表（不含指标，按体检日期倒序，最近 20 份）——智能体工具用
     *
     * @param memberId 当前登录账号（归属校验主体）
     * @return 报告快照列表（indicators 恒为 null）
     */
    @GetMapping("/reports")
    public R<List<HealthReportDTO>> listReports(@RequestParam("memberId") Long memberId,
                                                @RequestParam(value = "familyMemberId", required = false) Long familyMemberId) {
        return queryTemplate.query(HealthActions.INTERNAL_REPORT_LIST,
                () -> reportService.listInternal(memberId, familyMemberId));
    }
}
