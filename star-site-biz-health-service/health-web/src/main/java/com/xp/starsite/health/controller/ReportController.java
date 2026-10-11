package com.xp.starsite.health.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.health.dto.IndicatorBatchDTO;
import com.xp.starsite.health.dto.ReportCreateDTO;
import com.xp.starsite.health.service.ReportService;
import com.xp.starsite.health.vo.HealthReportVO;
import com.xp.starsite.health.vo.PageVO;
import com.xp.starsite.health.vo.TrendPointVO;
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
 * 体检报告接口：报告建档/分页/详情/删除、指标批量录入、关键词搜索、报告指标趋势。
 * <p>
 * 拆分约定：health-web 按 REST 资源一资源一 Controller（报告/就医/日常指标/孕期/AI 解读），
 * 类级统一挂 /api/health，资源边界由方法级路径表达——历史 URL 保持逐字节不变（小程序在用）。
 * 全部端点需 JWT；memberId 从网关注入的 JWT attribute 取，用于归属校验防横向越权。
 * AI 解读与 OCR 见 {@link ReportInsightController}。
 * <p>
 * 业务逻辑统一经模板包裹：模板包掉成功与失败的全部响应处理（成功 R.ok / 失败 R.fail），
 * Controller 一行返回；动作命名规范 health.report.*。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 上传报告元数据（建档；标题缺省为「&lt;年份&gt;年体检报告」）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param dto      报告元数据
     * @return 新建报告视图
     */
    @PostMapping("/reports")
    public R<HealthReportVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                    @Valid @RequestBody ReportCreateDTO dto) {
        return writeTemplate.write("health.report.create",
                () -> reportService.createReport(memberId, dto));
    }

    /**
     * 报告分页列表（按体检日期倒序；不传 familyMemberId 时限定全家成员范围）
     *
     * @param familyMemberId 可选，指定成员单查
     * @param year           可选，按报告年份过滤
     */
    @GetMapping("/reports")
    public R<PageVO<HealthReportVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                          @RequestParam(required = false) Long familyMemberId,
                                          @RequestParam(required = false) Integer year,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size) {
        return queryTemplate.query("health.report.page",
                () -> reportService.listReports(memberId, familyMemberId, year, page, size));
    }

    /**
     * 报告详情（含全部指标；同时承载 AI 异步解读结果的轮询）
     *
     * @param id 报告 id
     */
    @GetMapping("/reports/{id}")
    public R<HealthReportVO> detail(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                    @PathVariable Long id) {
        return queryTemplate.query("health.report.detail",
                () -> reportService.getDetail(memberId, id));
    }

    /**
     * 批量录入报告指标（重录覆盖；异常标志按参考区间自动判定）
     *
     * @param id 报告 id
     */
    @PostMapping("/reports/{id}/indicators")
    public R<Void> addIndicators(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                 @PathVariable Long id,
                                 @Valid @RequestBody IndicatorBatchDTO dto) {
        return writeTemplate.write("health.report.indicator.batch",
                () -> reportService.addIndicators(memberId, id, dto));
    }

    /**
     * 删除报告（逻辑删）
     *
     * @param id 报告 id
     */
    @DeleteMapping("/reports/{id}")
    public R<Void> delete(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        return writeTemplate.write("health.report.delete",
                () -> reportService.deleteReport(memberId, id));
    }

    /**
     * 关键词搜索报告：命中标题/医院/总结/指标名，限定自己家庭成员范围，最多 50 条
     *
     * @param keyword 关键词
     */
    @GetMapping("/search")
    public R<List<HealthReportVO>> search(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                          @RequestParam String keyword,
                                          @RequestParam(required = false) Long familyMemberId) {
        return queryTemplate.query("health.report.search",
                () -> reportService.search(memberId, keyword, familyMemberId));
    }

    /**
     * 报告指标历年趋势（按报告年份升序，折线图数据）
     *
     * @param itemName 指标名
     */
    @GetMapping("/trends")
    public R<List<TrendPointVO>> trends(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                        @RequestParam Long familyMemberId,
                                        @RequestParam String itemName) {
        return queryTemplate.query("health.report.trend",
                () -> reportService.trends(memberId, familyMemberId, itemName));
    }
}
