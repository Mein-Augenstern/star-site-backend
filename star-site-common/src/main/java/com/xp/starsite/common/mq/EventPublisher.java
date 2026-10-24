package com.xp.starsite.common.mq;

/**
 * 领域事件发布器（业务服务面向此接口编程，实现可替换/降级）
 * <p>
 * 使用方在配置类里 @Import(StarSiteMqConfig.class) 并配置 rocketmq.name-server 启用。
 *
 * @author muyi
 * @since 2026-10
 */
public interface EventPublisher {

    /**
     * 普通事件：尽力送达，不保证与本地事务一致（适合可丢失的通知类事件）
     */
    void publish(String topic, String eventType, String bizType, Long bizId, Object payload);

    /**
     * 事务事件：半消息发送成功后执行 localTransaction，
     * 正常返回则提交消息（消费者可见），抛异常则回滚消息（丢弃）。
     * 适用「业务落库 + 发事件」需要原子的场景。
     */
    void publishTransactional(String topic, String eventType, String bizType, Long bizId, Object payload,
                              LocalTransactionExecutor localTransaction);
}
