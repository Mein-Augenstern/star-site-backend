package com.xp.starsite.common.async;

/** 异步命令执行器：业务实现为本组件 Bean，type() 即路由键
 *
 * @author muyi
 * @since 2026-10
 */
public interface AsyncCommandExecutor {

    /** 命令类型路由键（同 asyncmd 的执行器注册语义） */
    String type();

    /**
     * 执行命令。正常返回 = 成功；抛异常 = 失败触发指数退避重试，超限进 DEAD。
     */
    void execute(AsyncCommand cmd) throws Exception;
}
