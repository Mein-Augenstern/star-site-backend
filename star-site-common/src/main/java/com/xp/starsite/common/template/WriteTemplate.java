package com.xp.starsite.common.template;

import com.xp.starsite.common.result.R;
import org.springframework.beans.factory.annotation.Value;

import java.util.function.Supplier;

/**
 * 写入/更新模板（写路径变体）：create/update/delete 等变更动作。
 * 事务仍由 Service 的 @Transactional 承担；模板包掉成功与失败处理
 * （成功 R.ok/R.voidOk，失败 R.fail），后续可加程序化事务/审计钩子，对调用方透明。
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

    /** 写动作（有返回值）：业务返回值经模板封装为 R.ok */
    public <T> R<T> write(String action, Supplier<T> fn) {
        return execute(action, fn);
    }

    /** 写动作（无返回值）：成功封装为 R.ok() */
    public R<Void> write(String action, Runnable fn) {
        return executeVoid(action, fn);
    }
}
