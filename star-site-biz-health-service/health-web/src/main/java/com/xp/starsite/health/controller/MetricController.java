package com.xp.starsite.health.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.health.dto.MetricBatchDTO;
import com.xp.starsite.health.service.MetricService;
import com.xp.starsite.health.vo.MetricTrendPoint;
import com.xp.starsite.health.vo.MetricVO;
import com.xp.starsite.health.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 日常指标接口：血压/血糖/体重等自测指标的录入、分页、删除与趋势曲线。
 * 注意与报告指标（{@link ReportController} 的 /reports/{id}/indicators 与 /trends）区分：
 * 本控制器读写 daily_metric（按日自测），报告指标读写体检报告单上的 health_indicator。
 * 拆分约定与鉴权说明见 {@link ReportController} 类注释；
 * 业务逻辑统一经模板包裹（动作名 health.metric.*）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class MetricController {

    private final MetricService metricService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 批量录入日常指标（血压一次产生高压+低压两条）
     */
    @PostMapping("/metrics")
    public R<List<MetricVO>> addMetrics(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                        @Valid @RequestBody MetricBatchDTO dto) {
        return R.ok(writeTemplate.execute("health.metric.batchAdd",
                () -> metricService.addMetrics(memberId, dto)));
    }

    /**
     * 日常指标分页列表（按测量日期倒序；不传 familyMemberId 时限定全家成员范围）
     */
    @GetMapping("/metrics")
    public R<PageVO<MetricVO>> listMetrics(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                           @RequestParam(required = false) Long familyMemberId,
                                           @RequestParam(required = false) String metricType,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "20") long size) {
        return R.ok(queryTemplate.execute("health.metric.page",
                () -> metricService.listMetrics(memberId, familyMemberId, metricType, page, size)));
    }

    /**
     * 删除日常指标（逻辑删）
     */
    @DeleteMapping("/metrics/{id}")
    public R<Void> deleteMetric(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                @PathVariable Long id) {
        writeTemplate.execute("health.metric.delete",
                () -> metricService.deleteMetric(memberId, id));
        return R.ok();
    }

    /**
     * 日常指标趋势（按测量日期升序；孕期体重曲线等场景）
     */
    @GetMapping("/metrics/trends")
    public R<List<MetricTrendPoint>> metricTrends(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                                  @RequestParam Long familyMemberId,
                                                  @RequestParam String metricType) {
        return R.ok(queryTemplate.execute("health.metric.trend",
                () -> metricService.metricTrends(memberId, familyMemberId, metricType)));
    }
}
