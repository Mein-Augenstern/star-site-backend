package com.xp.starsite.finance.controller.actions;

/**
 * 财务域动作名常量（模板留痕 action 字段，规范：服务.资源.动作）
 * <p>
 * 动作名会进模板留痕与 digest，只能新增、不能改名：字符串值与历史值逐字节一致。
 * 常量名取「资源.动作」段，省略服务前缀 finance.。
 *
 * @author muyi
 * @since 2026-10
 */
public final class FinanceActions {

    // —— 资金账户 ——

    /** 新建资金账户 */
    public static final String ACCOUNT_CREATE = "finance.account.create";

    /** 我的账户列表 */
    public static final String ACCOUNT_LIST = "finance.account.list";

    /** 账户详情 */
    public static final String ACCOUNT_DETAIL = "finance.account.detail";

    /** 修改账户信息 */
    public static final String ACCOUNT_UPDATE = "finance.account.update";

    /** 注销账户 */
    public static final String ACCOUNT_DELETE = "finance.account.delete";

    // —— 记账流水 ——

    /** 记一笔流水 */
    public static final String TRANSACTION_CREATE = "finance.transaction.create";

    /** 流水分页 */
    public static final String TRANSACTION_PAGE = "finance.transaction.page";

    /** 流水详情 */
    public static final String TRANSACTION_DETAIL = "finance.transaction.detail";

    /** 删除流水并回滚余额 */
    public static final String TRANSACTION_DELETE = "finance.transaction.delete";

    // —— 月度预算 ——

    /** 保存预算（upsert） */
    public static final String BUDGET_SAVE = "finance.budget.save";

    /** 账期预算列表 */
    public static final String BUDGET_LIST = "finance.budget.list";

    /** 删除预算 */
    public static final String BUDGET_DELETE = "finance.budget.delete";

    // —— 家庭目标 ——

    /** 新建储蓄目标 */
    public static final String GOAL_CREATE = "finance.goal.create";

    /** 我的目标列表 */
    public static final String GOAL_LIST = "finance.goal.list";

    /** 修改目标 */
    public static final String GOAL_UPDATE = "finance.goal.update";

    /** 删除目标 */
    public static final String GOAL_DELETE = "finance.goal.delete";

    // —— 三张表 ——

    /** 资产负债表 */
    public static final String REPORT_BALANCE_SHEET = "finance.report.balanceSheet";

    /** 收支报表 */
    public static final String REPORT_INCOME_EXPENSE = "finance.report.incomeExpense";

    /** 现金流量表 */
    public static final String REPORT_CASH_FLOW = "finance.report.cashFlow";

    /** 净资产趋势 */
    public static final String REPORT_NET_WORTH = "finance.report.netWorth";

    /** 分类占比 */
    public static final String REPORT_CATEGORY_SHARE = "finance.report.categoryShare";

    /** 月度收支趋势 */
    public static final String REPORT_MONTHLY_TREND = "finance.report.monthlyTrend";

    /** 立即落当日净资产快照 */
    public static final String REPORT_SNAPSHOT = "finance.report.snapshot";

    // —— AI 诊断 ——

    /** AI 财务健康诊断 */
    public static final String DIAGNOSIS_RUN = "finance.diagnosis.run";

    // —— 账单导入 ——

    /** 导入账单 CSV */
    public static final String IMPORT_START = "finance.import.start";

    /** 异步导入记录 */
    public static final String IMPORT_LOGS = "finance.import.logs";

    // —— 服务间内部接口 ——

    /** 资产负债表（服务间只读，智能体工具用） */
    public static final String INTERNAL_BALANCE_SHEET = "finance.internal.balanceSheet";

    private FinanceActions() {
    }
}
