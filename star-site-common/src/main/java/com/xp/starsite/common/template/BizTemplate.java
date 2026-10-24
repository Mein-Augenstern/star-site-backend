package com.xp.starsite.common.template;

import com.xp.starsite.common.error.CommonError;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.log.DigestLoggers;
import com.xp.starsite.common.log.TraceContext;
import com.xp.starsite.common.result.R;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * 统一业务模板骨架：所有对外暴露入口（Controller/Feign 内部接口）的标准写法。
 * <p>
 * 模板**包掉成功与失败的全部处理**——调用方拿到手就是最终响应 {@link R}，无需再包 R.ok：
 * <ul>
 *   <li>成功：业务返回值 → {@code R.ok(data)}；</li>
 *   <li>业务失败：BizException → {@code R.fail(code, 用户可读文案)}（warn 留痕）；</li>
 *   <li>系统异常：任意 RuntimeException → error 留痕（含堆栈）→ {@code R.fail(500, 系统繁忙)}，
 *       杜绝裸堆栈漏给调用方。</li>
 * </ul>
 * 每次执行同时留一行 server digest（动作名/读写类型/status/costMs/traceId），
 * 动作命名规范：{@code 服务.资源.动作}（如 health.report.page）。
 * 具体变体见 {@link QueryTemplate}（读）与 {@link WriteTemplate}（写）。
 *
 * @author muyi
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
     * 模板执行骨架：traceId 兜底 → 计时 → 业务执行 → digest 留痕 → 响应封装。
     * 成功与失败的处理全部在模板内完成，调用方直接返回结果即可。
     *
     * @param action 动作名，规范：服务.资源.动作（如 health.report.page）
     */
    public <T> R<T> execute(String action, Supplier<T> fn) {
        String traceId = TraceContext.current();
        long start = System.currentTimeMillis();
        try {
            T result = fn.get();
            emit(action, "success", null, System.currentTimeMillis() - start);
            return R.ok(result);
        } catch (BizException e) {
            emit(action, "biz_error", String.valueOf(e.getCode()), System.currentTimeMillis() - start);
            log.warn("业务拒绝: action={}, code={}, msg={}", action, e.getCode(), e.getMessage());
            // 5xx 语义的业务失败（依赖不可用等）对调用方可重试；4xx 参数/权限类重试无意义
            R<T> r = R.fail(e.getCode(), e.getMessage(), e.getCode() >= 500);
            r.withError(CommonError.of(e.getCode(), e.getClass().getName(), e.getMessage(), traceId));
            return r;
        } catch (RuntimeException e) {
            emit(action, "error", e.getClass().getSimpleName(), System.currentTimeMillis() - start);
            log.error("系统异常: action={}, traceId={}", action, traceId, e);
            R<T> r = R.fail(500, "系统繁忙，请稍后再试", true);
            r.withError(CommonError.ofThrowable(500, e, traceId));
            return r;
        }
    }

    /** 无返回值动作的封装（成功 R.ok()，失败语义同上） */
    public R<Void> executeVoid(String action, Runnable fn) {
        return execute(action, () -> {
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
