package com.xp.starsite.common.rpc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 宽容 RPC 标注：标在 Feign 客户端接口方法上，RpcClientAspect 对该方法的失败
 * （业务码非 0 / 依赖不可用）只告警不抛异常，R 原样透传（data 为 null）。
 * <p>
 * 语义：「提醒注册/通知触达」这类增值能力失败不阻断主流程——调用方拿到
 * code != 0 的 R 自行判断，无需 try-catch。
 * 未标注的方法一律 fail-closed：失败抛标准 RpcException（带 retryable）。
 * 值级标注在「客户端接口方法」上（提供方实现类不感知）。
 *
 * @author muyi
 * @since 2026-10
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcTolerant {
}
