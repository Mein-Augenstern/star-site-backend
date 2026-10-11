package com.xp.starsite.common.mq;

import lombok.extern.slf4j.Slf4j;

/**
 * EventPublisher 降级实现：MQ 停用（未配置 rocketmq.name-server）时兜底注册。
 * 发布为空操作并告警——事件丢失意味着消费方漏处理，需业务侧自行补偿
 * （如 reminder 降级为 @Scheduled 扫描、账单导入可走同步接口重传）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
public class NoopEventPublisher implements EventPublisher {

    public NoopEventPublisher() {
        log.info("MQ 停用：EventPublisher 降级为 Noop（事件发布为空操作）");
    }

    @Override
    public void publish(String topic, String eventType, String bizType, Long bizId, Object payload) {
        log.warn("MQ 停用，事件未发布: topic={}, type={}, bizType={}, bizId={}", topic, eventType, bizType, bizId);
    }

    @Override
    public void publishTransactional(String topic, String eventType, String bizType, Long bizId, Object payload,
                                     LocalTransactionExecutor localTransaction) {
        // 半消息发不出去 = 本地事务也不该做，直接拒绝而非静默吞掉
        throw new IllegalStateException("MQ 停用，事务事件不可用: topic=" + topic + ", type=" + eventType);
    }
}
