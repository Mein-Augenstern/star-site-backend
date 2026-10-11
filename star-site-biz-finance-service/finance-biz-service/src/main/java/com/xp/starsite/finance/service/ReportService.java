package com.xp.starsite.finance.service;

import com.xp.starsite.finance.vo.BalanceSheetVO;
import com.xp.starsite.finance.vo.CashFlowVO;
import com.xp.starsite.finance.vo.IncomeExpenseVO;
import com.xp.starsite.finance.vo.NetWorthPointVO;

import java.util.List;

/**
 * 三张表报表服务（全部为查询层聚合，无中间表）。
 * 首参 memberId 用于数据归属。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface ReportService {

    /** 资产负债表：账户资产 + 档案资产估值 / 负债账户 / 净资产 */
    BalanceSheetVO balanceSheet(Long memberId);

    /** 收支表：期间收入/支出/结余率（month 传 0 = 全年） */
    IncomeExpenseVO incomeExpense(Long memberId, int year, int month);

    /** 现金流量表（V1 简化）：流动账户余额 + 可撑月数 */
    CashFlowVO cashFlow(Long memberId);

    /** 净资产曲线 */
    List<NetWorthPointVO> netWorthTrend(Long memberId);

    /** 当月分类支出占比（饼图数据，降序） */
    List<com.xp.starsite.finance.vo.CategoryShareVO> categoryShare(Long memberId, int year, int month);

    /** 近 12 个月收支趋势（柱状图数据） */
    List<com.xp.starsite.finance.vo.MonthlyTrendVO> monthlyTrend(Long memberId);

    /** 立即落当日净资产快照（当前用户） */
    com.xp.starsite.finance.vo.NetWorthPointVO snapshotNow(Long memberId);

    /** 落当日净资产快照（uk(owner,date) 幂等）；定时任务与手动触发共用 */
    void snapshotToday(Long memberId);
}
