package com.xp.starsite.common.mq;

/**
 * 事务消息的本地事务回调：在半消息发送成功后执行。
 * 抛出任何异常都会导致消息回滚（业务方需自行保证本地事务已回滚）。
 */
@FunctionalInterface
public interface LocalTransactionExecutor {

    void execute() throws Exception;
}
