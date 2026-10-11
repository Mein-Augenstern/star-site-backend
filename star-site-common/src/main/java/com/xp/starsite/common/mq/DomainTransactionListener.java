package com.xp.starsite.common.mq;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 事务消息监听器：半消息送达后执行业务方本地事务，成功提交 / 异常回滚。
 * 使用原生 rocketmq-client API（非 rocketmq-spring 注解式），
 * 由 RocketMqEventPublisher 内置，业务方零感知。
 */
class DomainTransactionListener implements TransactionListener {

    private static final Logger log = LoggerFactory.getLogger(DomainTransactionListener.class);

    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        if (!(arg instanceof RocketMqEventPublisher.PendingTx tx)) {
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
        try {
            tx.executor().execute();
            return LocalTransactionState.COMMIT_MESSAGE;
        } catch (Exception e) {
            log.error("本地事务执行失败，回滚半消息: eventId={}", tx.eventId(), e);
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }

    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        // 回查时本地事务早已结束、结果无法核实：固定偏向 COMMIT。
        // 理由：本项目的跨域事件均为 upsert/幂等语义，多发一条的代价是消费端去重；漏发则丢联动。
        return LocalTransactionState.COMMIT_MESSAGE;
    }
}
