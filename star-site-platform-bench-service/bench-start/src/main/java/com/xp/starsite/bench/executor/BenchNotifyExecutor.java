package com.xp.starsite.bench.executor;

import com.xp.starsite.asynccmd.executor.AsyncCommandExecutor;
import com.xp.starsite.asynccmd.model.AsyncCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 压测用异步命令执行器：模拟通知发送（正常返回=成功，可注入异常测试重试）。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
public class BenchNotifyExecutor implements AsyncCommandExecutor {

    /** 日志 */
    private static final Logger log = LoggerFactory.getLogger(BenchNotifyExecutor.class);

    @Override
    public String type() {
        return "bench-notify";
    }

    @Override
    public void execute(AsyncCommand cmd) {
        log.debug("bench-notify: bizKey={}, payload={}", cmd.getBizKey(), cmd.getPayload());
        // 正常返回 = 成功；如需测试重试：throw new RuntimeException("simulated failure");
    }
}

