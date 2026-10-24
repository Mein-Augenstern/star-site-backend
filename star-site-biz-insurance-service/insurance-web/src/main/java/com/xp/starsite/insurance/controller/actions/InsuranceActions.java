package com.xp.starsite.insurance.controller.actions;

/**
 * 保障域动作名常量（模板留痕 action 字段，规范：服务.资源.动作）
 * <p>
 * 动作名会进模板留痕与 digest，只能新增、不能改名：字符串值与历史值逐字节一致。
 * 常量名取「资源.动作」段，省略服务前缀 insurance.。
 *
 * @author muyi
 * @since 2026-10
 */
public final class InsuranceActions {

    // —— 保单 ——

    /** 保单录入 */
    public static final String POLICY_CREATE = "insurance.policy.create";

    /** 保单分页 */
    public static final String POLICY_PAGE = "insurance.policy.page";

    /** 保单详情 */
    public static final String POLICY_DETAIL = "insurance.policy.detail";

    /** 保单更新 */
    public static final String POLICY_UPDATE = "insurance.policy.update";

    /** 保单删除 */
    public static final String POLICY_DELETE = "insurance.policy.delete";

    // —— 保障地图 ——

    /** 保障地图（按家庭成员聚合的保障概览） */
    public static final String COVERAGE_MAP = "insurance.map";

    private InsuranceActions() {
    }
}
