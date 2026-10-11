package com.xp.starsite.common.template;

import org.springframework.beans.factory.annotation.Value;

/**
 * 写入/更新模板（写路径变体）：create/update/delete 等变更动作。
 * 事务仍由 Service 的 @Transactional 承担；本模板负责写路径留痕与异常归一，
 * 后续可加程序化事务/审计钩子，对调用方透明。
 *
 * @author xiaopu
 * @since 2026-10
 */
public class WriteTemplate extends BizTemplate {

    public WriteTemplate(@Value("${spring.application.name:unknown}") String appName) {
        super(appName);
    }

    @Override
    protected String spanType() {
        return "write";
    }
}
