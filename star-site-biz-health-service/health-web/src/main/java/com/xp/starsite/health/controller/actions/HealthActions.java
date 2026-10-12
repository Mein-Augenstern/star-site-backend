package com.xp.starsite.health.controller.actions;

/**
 * 健康域动作名常量（模板留痕 action 字段，规范：服务.资源.动作）
 * <p>
 * 动作名会进模板留痕与 digest，只能新增、不能改名：字符串值与历史值逐字节一致。
 * 常量名取「资源.动作」段，省略服务前缀 health.。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class HealthActions {

    // —— 体检报告 ——

    /** 报告建档 */
    public static final String REPORT_CREATE = "health.report.create";

    /** 报告分页 */
    public static final String REPORT_PAGE = "health.report.page";

    /** 报告详情（含 AI 解读结果轮询） */
    public static final String REPORT_DETAIL = "health.report.detail";

    /** 报告指标批量录入 */
    public static final String REPORT_INDICATOR_BATCH = "health.report.indicator.batch";

    /** 报告删除 */
    public static final String REPORT_DELETE = "health.report.delete";

    /** 报告关键词搜索 */
    public static final String REPORT_SEARCH = "health.report.search";

    /** 报告指标历年趋势 */
    public static final String REPORT_TREND = "health.report.trend";

    // —— 就医记录 ——

    /** 就医记录新建 */
    public static final String VISIT_CREATE = "health.visit.create";

    /** 就医记录分页 */
    public static final String VISIT_PAGE = "health.visit.page";

    /** 就医记录删除 */
    public static final String VISIT_DELETE = "health.visit.delete";

    // —— 日常指标 ——

    /** 日常指标批量录入 */
    public static final String METRIC_BATCH_ADD = "health.metric.batchAdd";

    /** 日常指标分页 */
    public static final String METRIC_PAGE = "health.metric.page";

    /** 日常指标删除 */
    public static final String METRIC_DELETE = "health.metric.delete";

    /** 日常指标趋势 */
    public static final String METRIC_TREND = "health.metric.trend";

    // —— 孕期助手 ——

    /** 孕期档案设置/更新 */
    public static final String PREGNANCY_UPSERT = "health.pregnancy.upsert";

    /** 孕期档案查询 */
    public static final String PREGNANCY_GET = "health.pregnancy.get";

    // —— 报告智能解读 ——

    /** AI 解读报告（同步） */
    public static final String INSIGHT_INTERPRET = "health.insight.interpret";

    /** AI 解读报告（异步） */
    public static final String INSIGHT_INTERPRET_ASYNC = "health.insight.interpretAsync";

    /** 拍照 OCR 结构化 */
    public static final String INSIGHT_OCR = "health.insight.ocr";

    private HealthActions() {
    }
}
