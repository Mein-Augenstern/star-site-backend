package com.xp.starsite.finance.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
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

    @GetMapping("/balance-sheet")
    public R<BalanceSheetVO> balanceSheet(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(reportService.balanceSheet(memberId));
    }

    /** month 传 0 = 全年 */
    @GetMapping("/income-expense")
    public R<IncomeExpenseVO> incomeExpense(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                            @RequestParam int year,
                                            @RequestParam(defaultValue = "0") int month) {
        return R.ok(reportService.incomeExpense(memberId, year, month));
    }

    @GetMapping("/cash-flow")
    public R<CashFlowVO> cashFlow(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(reportService.cashFlow(memberId));
    }

    @GetMapping("/net-worth-trend")
    public R<List<NetWorthPointVO>> netWorthTrend(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(reportService.netWorthTrend(memberId));
    }

    @GetMapping("/category-share")
    public R<List<com.xp.starsite.finance.vo.CategoryShareVO>> categoryShare(
            @RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
            @RequestParam int year,
            @RequestParam int month) {
        return R.ok(reportService.categoryShare(memberId, year, month));
    }

    @GetMapping("/monthly-trend")
    public R<List<com.xp.starsite.finance.vo.MonthlyTrendVO>> monthlyTrend(
            @RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(reportService.monthlyTrend(memberId));
    }

    /** 立即落当日净资产快照（当前用户） */
    @PostMapping("/snapshot-now")
    public R<NetWorthPointVO> snapshotNow(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(reportService.snapshotNow(memberId));
    }

    /** AI 财务健康诊断（实时聚合三张表 → LLM） */
    @PostMapping("/health-diagnosis")
    public R<String> healthDiagnosis(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(diagnosisService.diagnose(memberId));
    }
}
