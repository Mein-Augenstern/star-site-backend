package com.xp.starsite.common.template;

import com.xp.starsite.common.result.R;
import org.springframework.beans.factory.annotation.Value;

import java.util.function.Supplier;

/**
 * 查询模板（读路径变体）：列表/详情/搜索/趋势等只读动作。
 * 模板包掉成功与失败处理，直接返回最终响应；与写路径分开计数观测，
 * 后续可在此加缓存钩子，对调用方透明。
 * 用法（Controller）：{@code return queryTemplate.query("health.report.page", () -> ...);}
 *
 * @author muyi
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

    /** 查询动作：业务返回值经模板封装为 R.ok */
    public <T> R<T> query(String action, Supplier<T> fn) {
        return execute(action, fn);
    }
}
