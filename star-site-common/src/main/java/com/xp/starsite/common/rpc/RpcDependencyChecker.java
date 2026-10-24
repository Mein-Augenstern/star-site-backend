package com.xp.starsite.common.rpc;

import org.springframework.cloud.openfeign.FeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * RPC 依赖注入校验器：容器单例就绪后，扫描全部标注 {@link RpcIntegration} 的 bean，
 * 反射校验其 Feign 客户端类型字段注入非空——缺失即 fail-fast，把「带着断掉的依赖上线」
 * 拦在启动期；同时输出一行本服务 RPC 依赖面清单（集成类 → 依赖服务），留档可查。
 *
 * @author muyi
 * @since 2026-10
 */
public class RpcDependencyChecker implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(RpcDependencyChecker.class);

    private final ApplicationContext context;

    public RpcDependencyChecker(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void afterSingletonsInstantiated() {
        String app = context.getEnvironment().getProperty("spring.application.name", "unknown");
        List<String> beans = new ArrayList<>(List.of(context.getBeanNamesForAnnotation(RpcIntegration.class)));
        if (beans.isEmpty()) {
            log.info("RPC 依赖面: app={} 无 @RpcIntegration 集成出口", app);
            return;
        }
        List<String> deps = new ArrayList<>();
        for (String beanName : beans) {
            Object bean = context.getBean(beanName);
            RpcIntegration marker = bean.getClass().getAnnotation(RpcIntegration.class);
            String targets = marker == null || marker.value().length == 0 ? "未声明" : String.join(",", marker.value());
            deps.add(bean.getClass().getSimpleName() + "->" + targets);
            checkFeignFieldsInjected(beanName, bean);
        }
        log.info("RPC 依赖面: app={}, 集成出口={}", app, deps);
    }

    /** 校验集成 bean 的全部 Feign 客户端字段已注入（非 null），缺失抛 IllegalStateException 终止启动 */
    private void checkFeignFieldsInjected(String beanName, Object bean) {
        for (Class<?> type = bean.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.getType().isAnnotationPresent(FeignClient.class)) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    if (field.get(bean) == null) {
                        throw new IllegalStateException(
                                "RPC 依赖注入不完整: bean=" + beanName + ", 字段=" + field.getName()
                                        + "（类型 " + field.getType().getSimpleName() + "）为 null，拒绝带断依赖启动");
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("RPC 依赖校验失败: bean=" + beanName, e);
                }
            }
        }
    }
}
