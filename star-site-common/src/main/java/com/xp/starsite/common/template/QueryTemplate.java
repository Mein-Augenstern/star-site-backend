package com.xp.starsite.common.template;

import org.springframework.beans.factory.annotation.Value;

/**
 * 查询模板（读路径变体）：列表/详情/搜索/趋势等只读动作。
 * 与写路径分开计数观测；后续可在此加缓存钩子，对调用方透明。
 * 用法（Controller）：{@code return R.ok(queryTemplate.run("health.report.page", () -> ...));}
 *
 * @author xiaopu
 * @since 2026-10
 */
public class QueryTemplate extends BizTemplate {

    public QueryTemplate(@Value("${spring.application.name:unknown}") String appName) {
        super(appName);
    }

    @Override
    protected String spanType() {
        return "read";
    }
}
