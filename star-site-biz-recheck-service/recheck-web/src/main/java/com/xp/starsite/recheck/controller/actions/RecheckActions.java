package com.xp.starsite.recheck.controller.actions;

/**
 * 复查闭环域动作名常量（模板留痕 action 字段，规范：服务.资源.动作）
 * <p>
 * 动作名会进模板留痕与 digest，只能新增、不能改名：字符串值与历史值逐字节一致。
 * 常量名取「资源.动作」段，省略服务前缀 recheck.。
 *
 * @author muyi
 * @since 2026-10
 */
public final class RecheckActions {

    // —— 复查计划 ——

    /** 从报告指标创建复查计划 */
    public static final String PLAN_CREATE = "recheck.plan.create";

    /** 复查计划列表 */
    public static final String PLAN_LIST = "recheck.plan.list";

    /** 关联复查新报告闭环 */
    public static final String PLAN_CLOSE = "recheck.plan.close";

    /** 取消复查计划 */
    public static final String PLAN_CANCEL = "recheck.plan.cancel";

    private RecheckActions() {
    }
}
