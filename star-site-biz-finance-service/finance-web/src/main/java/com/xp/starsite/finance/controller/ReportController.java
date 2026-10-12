package com.xp.starsite.finance.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.finance.controller.actions.FinanceActions;
import com.xp.starsite.finance.service.ReportService;
import com.xp.starsite.finance.vo.BalanceSheetVO;
import com.xp.starsite.finance.vo.CashFlowVO;
import com.xp.starsite.finance.vo.IncomeExpenseVO;
import com.xp.starsite.finance.vo.NetWorthPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 三张表接口（全部需 JWT；数据按当前账号隔离）
 * <p>
 * 业务逻辑统一经模板包裹：模板包掉成功与失败的全部响应处理（成功 R.ok / 失败 R.fail），
 * Controller 一行返回；动作命名规范 finance.report.* / finance.diagnosis.*。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/finance/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final com.xp.starsite.finance.ai.FinanceDiagnosisService diagnosisService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 资产负债表（总资产/总负债/净资产及构成）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @return 资产负债表视图
     */
    @GetMapping("/balance-sheet")
    public R<BalanceSheetVO> balanceSheet(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query(FinanceActions.REPORT_BALANCE_SHEET,
                () -> reportService.balanceSheet(memberId));
    }

    /**
     * 收支报表（分类汇总；month 传 0 = 全年）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param year     年份
     * @param month    月份（0 = 全年）
     * @return 收支报表视图
     */
    @GetMapping("/income-expense")
    public R<IncomeExpenseVO> incomeExpense(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                            @RequestParam int year,
                                            @RequestParam(defaultValue = "0") int month) {
        return queryTemplate.query(FinanceActions.REPORT_INCOME_EXPENSE,
                () -> reportService.incomeExpense(memberId, year, month));
    }

    /**
     * 现金流量表（本月/本年现金流概览）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @return 现金流视图
     */
    @GetMapping("/cash-flow")
    public R<CashFlowVO> cashFlow(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query(FinanceActions.REPORT_CASH_FLOW,
                () -> reportService.cashFlow(memberId));
    }

    /**
     * 净资产趋势（按日快照，折线图数据）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @return 净资产快照点列表
     */
    @GetMapping("/net-worth-trend")
    public R<List<NetWorthPointVO>> netWorthTrend(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query(FinanceActions.REPORT_NET_WORTH,
                () -> reportService.netWorthTrend(memberId));
    }

    /**
     * 分类占比（指定账期支出分类占比，饼图数据）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param year     年份
     * @param month    月份
     * @return 分类占比列表
     */
    @GetMapping("/category-share")
    public R<List<com.xp.starsite.finance.vo.CategoryShareVO>> categoryShare(
            @RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
            @RequestParam int year,
            @RequestParam int month) {
        return queryTemplate.query(FinanceActions.REPORT_CATEGORY_SHARE,
                () -> reportService.categoryShare(memberId, year, month));
    }

    /**
     * 月度收支趋势（近 12 个月，折线图数据）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @return 月度趋势点列表
     */
    @GetMapping("/monthly-trend")
    public R<List<com.xp.starsite.finance.vo.MonthlyTrendVO>> monthlyTrend(
            @RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query(FinanceActions.REPORT_MONTHLY_TREND,
                () -> reportService.monthlyTrend(memberId));
    }

    /**
     * 立即落当日净资产快照（当前用户）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @return 快照点（当日净资产）
     */
    @PostMapping("/snapshot-now")
    public R<NetWorthPointVO> snapshotNow(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return writeTemplate.write(FinanceActions.REPORT_SNAPSHOT,
                () -> reportService.snapshotNow(memberId));
    }

    /**
     * AI 财务健康诊断（实时聚合三张表 → LLM；无落库）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @return 诊断结论文本
     */
    @PostMapping("/health-diagnosis")
    public R<String> healthDiagnosis(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query(FinanceActions.DIAGNOSIS_RUN,
                () -> diagnosisService.diagnose(memberId));
    }
}
