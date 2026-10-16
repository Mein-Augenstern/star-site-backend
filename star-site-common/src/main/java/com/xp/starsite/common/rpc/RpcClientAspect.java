package com.xp.starsite.common.rpc;

import com.xp.starsite.common.result.R;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 统一 RPC 切面（AOP，替代显式 RpcCallGuard 调用）：自动拦截全平台所有 Feign 客户端
 * （接口名以 Api 结尾）的方法执行——
 * <ul>
 *   <li>摘要留痕：出站耗时/成败由 FeignDigestLogger（HTTP 层）承担，本切面补业务语义失败日志；</li>
 *   <li>统一异常转换：传输失败（连接/超时/HTTP 5xx/404）与业务失败（R.code != 0）
 *       一律翻译为标准 {@link RpcException}（带 retryable）——integration 层零 try-catch；</li>
 *   <li>宽容语义：客户端方法标 {@link RpcTolerant} 的（提醒注册/通知触达等增值能力），
 *       失败只告警不抛——合成失败 R（code != 0，data null）原样透传，调用方按需判断。</li>
 * </ul>
 * 可重试判定：R.retryable 声明优先，缺省按段位推断（连接/超时/404 未就绪/429/5xx → true）。
 * 条件装配：@ConditionalOnClass(FeignException)——有 Feign 运行时的服务才织入，
 * 网关/WebFlux 零感知（与 StarSiteFeignLogConfig 同款模式）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Aspect
@Component
@ConditionalOnClass(FeignException.class)
public class RpcClientAspect {

    /** 切全平台所有 Feign 客户端接口（命名规约：*Api，契约在 *-facade / 消费方 rpc 包） */
    @Around("execution(* com.xp.starsite..*Api.*(..))")
    public Object aroundRpcCall(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String action = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            if (result instanceof R<?> r && r.getCode() != 0) {
                boolean retryable = r.getRetryable() != null ? r.getRetryable() : r.getCode() >= 500;
                log.warn("[rpc] 业务失败: action={}, code={}, msg={}, retryable={}, cost={}ms",
                        action, r.getCode(), r.getMessage(), retryable, System.currentTimeMillis() - start);
                if (!isTolerant(method)) {
                    throw new RpcException(r.getCode(), r.getMessage(), retryable);
                }
            }
            return result;
        } catch (RpcException e) {
            throw e;
        } catch (Throwable e) {
            boolean retryable = classify(e);
            log.error("[rpc] 调用失败（统一翻译为 RpcException）: action={}, err={}, retryable={}, cost={}ms",
                    action, e.getMessage(), retryable, System.currentTimeMillis() - start, e);
            if (isTolerant(method)) {
                return R.fail(503, "依赖服务暂不可用", true);
            }
            throw new RpcException(500, "依赖服务暂不可用，请稍后再试", retryable, e);
        }
    }

    /** 宽容方法标注（Feign 接口方法上的 @RpcTolerant）→ 失败吞掉 */
    private boolean isTolerant(Method method) {
        return method.isAnnotationPresent(RpcTolerant.class);
    }

    /** 可重试推断：限流/未就绪（404）/5xx/网络层可重试；其余 4xx（参数/权限）重试无意义 */
    static boolean classify(Throwable e) {
        if (e instanceof FeignException fe) {
            int status = fe.status();
            if (status == 429 || status == 404 || status >= 500 || status == 0) {
                return true;
            }
            if (status >= 400 && status < 500) {
                return false;
            }
        }
        return true;
    }
}
