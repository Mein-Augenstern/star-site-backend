package com.xp.starsite.common.rpc;

import com.xp.starsite.common.result.R;
import com.xp.starsite.common.result.ResultCode;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * 统一 RPC 调用门卫（编程式切面，与 BizTemplate 同构）：integration 出口的唯一调用姿势。
 * <p>
 * 把散在各处的「判 code / 翻译异常 / 容错策略」收敛为两种语义：
 * <ul>
 *   <li>{@link #call} = fail-closed：Feign 异常/超时/code!=0 一律翻译成
 *       {@link RpcException}（带 retryable），强依赖路径用；</li>
 *   <li>{@link #attempt} = tolerant：失败吞掉、error 留痕、返回 false——
 *       「提醒是增值能力，不阻断主流程」这类弱依赖路径的统一写法。</li>
 * </ul>
 * 可重试判定：响应体声明的 {@code R.retryable} 优先，缺省按段位推断
 * （连接/超时、404 未就绪、5xx → true；其余 4xx → false）。
 * 出站耗时/状态留痕由 FeignDigestLogger（HTTP 层）承担，本类只补失败语义日志。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
public class RpcCallGuard {

    /**
     * fail-closed 调用：成功返回 data；任何失败抛 {@link RpcException}
     *
     * @param action 动作名（规范：调用方.资源.动作，如 recheck.health.reportDetail），留痕与日志用
     */
    public <T> T call(String action, Supplier<R<T>> call) {
        try {
            R<T> r = call.get();
            if (r == null) {
                throw new RpcException(ResultCode.SYSTEM_ERROR.getCode(), "依赖服务无响应", true);
            }
            if (r.getCode() == ResultCode.SUCCESS.getCode()) {
                return r.getData();
            }
            boolean retryable = r.getRetryable() != null ? r.getRetryable() : r.getCode() >= 500;
            if (r.getError() != null) {
                // 远端统一错误对象：类型与 traceId 进本地日志，排障可跨服务直达
                log.warn("RPC 业务失败: action={}, code={}, msg={}, remoteType={}, remoteTraceId={}, retryable={}",
                        action, r.getCode(), r.getMessage(), r.getError().getType(), r.getError().getTraceId(), retryable);
            } else {
                log.warn("RPC 业务失败: action={}, code={}, msg={}, retryable={}", action, r.getCode(), r.getMessage(), retryable);
            }
            throw new RpcException(r.getCode(), r.getMessage(), retryable);
        } catch (RpcException e) {
            throw e;
        } catch (Exception e) {
            boolean retryable = classify(e);
            log.warn("RPC 调用失败（fail-closed）: action={}, err={}, retryable={}", action, e.getMessage(), retryable);
            throw new RpcException(ResultCode.SYSTEM_ERROR.getCode(), "依赖服务暂不可用，请稍后再试", retryable, e);
        }
    }

    /**
     * tolerant 调用：成功（含业务码成功）返回 true；任何失败吞掉、error 留痕、返回 false。
     * 仅用于「增值能力不阻断主流程」的弱依赖（提醒注册、通知触达等），主链路禁止使用。
     */
    public boolean attempt(String action, Supplier<?> call) {
        try {
            call.get();
            return true;
        } catch (Exception e) {
            log.error("RPC 调用失败（tolerant，不阻断主流程）: action={}, err={}", action, e.getMessage(), e);
            return false;
        }
    }

    /** 可重试推断：限流/未就绪（404）/5xx/网络层可重试；其余 4xx（参数/权限）重试无意义 */
    static boolean classify(Throwable e) {
        try {
            if (e instanceof FeignException fe) {
                int status = fe.status();
                if (status == 429 || status == 404 || status >= 500 || status == 0) {
                    return true;
                }
                if (status >= 400 && status < 500) {
                    return false;
                }
            }
        } catch (NoClassDefFoundError ignore) {
            // 无 feign 类路径（消费方纯单测环境）：按可重试兜底；生产环境 feign 恒在，不走此分支
        }
        return true;
    }
}
