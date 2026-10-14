package com.xp.starsite.recheck.integration;

import com.xp.starsite.api.client.HealthApi;
import com.xp.starsite.api.dto.health.HealthReportDTO;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * health 横向服务集成层（SOFA integration 层）：recheck 域读报告/指标的唯一出口。
 * 领域内其他类禁止直接依赖 health 的 Feign——垂直依赖横向只走本层。
 * 与提醒不同，报告读取是复查闭环的强依赖（建计划/闭环都靠它），fail-closed。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthIntegration {

    private final HealthApi healthApi;

    /**
     * 报告详情（含全部指标）
     *
     * @throws BizException health 不可用（fail-closed）或报告不存在（透传 400 语义）
     */
    public HealthReportDTO getReportDetail(Long memberId, Long reportId) {
        R<HealthReportDTO> r;
        try {
            r = healthApi.getReportDetail(memberId, reportId);
        } catch (Exception e) {
            log.error("调用 health 服务失败（fail-closed）: reportId={}", reportId, e);
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "健康服务暂不可用，请稍后再试");
        }
        if (r == null || r.getCode() != ResultCode.SUCCESS.getCode()) {
            String message = r == null ? "健康服务返回异常，请稍后再试" : r.getMessage();
            int code = r == null ? ResultCode.SYSTEM_ERROR.getCode() : r.getCode();
            throw new BizException(code, message);
        }
        return r.getData();
    }
}
