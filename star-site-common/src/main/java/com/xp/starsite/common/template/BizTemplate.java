package com.xp.starsite.common.template;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.log.DigestLoggers;
import com.xp.starsite.common.log.TraceContext;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * 统一业务模板骨架：所有对外暴露入口（Controller/Feign 内部接口）包裹业务逻辑的标准姿势。
 * <p>
 * 统一三件事——① 留痕：每个动作一行 server digest（动作名/读写类型/状态/耗时/traceId）；
 * ② 异常归一：BizException 语义化上抛（全局异常处理器转用户文案），其他异常一律记
 * error 日志后归一为系统异常，杜绝裸堆栈漏出；③ 动作命名规范：
 * {@code 服务.资源.动作}（如 health.report.page），与日志平台检索对齐。
 * <p>
 * 具体变体见 {@link QueryTemplate}（读路径）与 {@link WriteTemplate}（写路径）——
 * 读写分开计数观测，后续可在各自变体加钩子（读：缓存；写：程序化事务/审计），对调用方透明。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
public abstract class BizTemplate {

    private final String appName;

    protected BizTemplate(String appName) {
        this.appName = appName;
    }

    /** span 类型标记（read/write），digest 留痕用 */
    protected abstract String spanType();

    /**
     * 模板执行骨架：traceId 兜底 → 计时 → 业务执行 → digest 留痕；
     * BizException 原样上抛（warn 级），未知异常 error 归一为系统异常。
     *
     * @param action 动作名，规范：服务.资源.动作（如 health.report.page）
     */
    public <T> T execute(String action, Supplier<T> fn) {
        String traceId = TraceContext.current();
        long start = System.currentTimeMillis();
        try {
            T result = fn.get();
            emit(action, "success", null, System.currentTimeMillis() - start);
            return result;
        } catch (BizException e) {
            emit(action, "biz_error", String.valueOf(e.getCode()), System.currentTimeMillis() - start);
            log.warn("业务拒绝: action={}, code={}, msg={}", action, e.getCode(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            emit(action, "error", e.getClass().getSimpleName(), System.currentTimeMillis() - start);
            log.error("系统异常: action={}, traceId={}", action, traceId, e);
            throw new BizException(500, "系统繁忙，请稍后再试");
        }
    }

    /** 无返回值版本 */
    public void execute(String action, Runnable fn) {
        execute(action, () -> {
            fn.run();
            return null;
        });
    }

    private void emit(String action, String status, String extra, long costMs) {
        DigestLoggers.emit(DigestLoggers.RPC_SERVER, DigestLoggers.fields(
                "span", spanType(), "action", action, "status", status,
                "code", extra, "costMs", costMs, "app", appName));
    }
}
