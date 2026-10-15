package com.xp.starsite.common.async;

import com.xp.starsite.common.async.mapper.AsyncCommandMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异步命令提交入口：在业务事务内调用 submit，命令与业务变更同事务原子落库
 * （asyncmd 的立身之本），提交方只管写，调度器负责可靠执行与重试。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@RequiredArgsConstructor
public class AsyncCommandService {

    private final AsyncCommandMapper mapper;

    @Value("${spring.application.name:unknown}")
    private String appName;

    /** 立即执行 */
    public AsyncCommand submit(String cmdType, String bizKey, String payloadJson) {
        return submitAt(cmdType, bizKey, payloadJson, LocalDateTime.now());
    }

    /** 延迟执行（延迟队列场景，如"30 分钟未支付取消订单"） */
    public AsyncCommand submitAt(String cmdType, String bizKey, String payloadJson, LocalDateTime runAt) {
        AsyncCommand cmd = new AsyncCommand();
        cmd.setCmdType(cmdType);
        cmd.setBizKey(bizKey == null ? "" : bizKey);
        cmd.setPayload(payloadJson);
        cmd.setStatus(AsyncCommand.PENDING);
        cmd.setMaxRetry(5);
        cmd.setRetryCount(0);
        cmd.setNextRunAt(runAt);
        cmd.setOwner(appName);
        mapper.insert(cmd);
        log.info("async cmd submitted: type={}, key={}, id={}, runAt={}", cmdType, bizKey, cmd.getId(), runAt);
        return cmd;
    }

    /** 幂等提交：同 type+bizKey 已有未完成命令时跳过（防重复触发，如定时任务反复提交） */
    public AsyncCommand submitIdempotent(String cmdType, String bizKey, String payloadJson) {
        if (mapper.countUnfinished(cmdType, bizKey) > 0) {
            return null;
        }
        return submit(cmdType, bizKey, payloadJson);
    }

    public List<AsyncCommand> findByIds(List<Long> ids) {
        return ids.isEmpty() ? List.of() : mapper.selectByIds(ids);
    }

    /** 按 id 取命令（管控处置前置查询，不存在返回 null） */
    public AsyncCommand findById(long id) {
        return mapper.selectById(id);
    }

    /** 管控处置回写（重试/重占/终止的统一状态落库） */
    public void updateState(AsyncCommand cmd) {
        mapper.updateState(cmd);
    }

    /** 管控端：各状态计数 */
    public long countByStatus(String status) {
        return mapper.countByStatus(status);
    }

    /** 管控端：积压（到期未执行 PENDING 数） */
    public long countBacklogDue(LocalDateTime now) {
        return mapper.countBacklogDue(now);
    }

    /** 管控端：近 N 条命令（按 id 倒序） */
    public List<AsyncCommand> selectRecent(int limit) {
        return mapper.selectRecent(limit);
    }

    /** 管控端：分页计数 */
    public long countPage(String status, String cmdType) {
        return mapper.countPage(status, cmdType);
    }

    /** 管控端：分页查询 */
    public List<AsyncCommand> selectPage(String status, String cmdType, long offset, long size) {
        return mapper.selectPage(status, cmdType, offset, size);
    }

    public void markSuccess(AsyncCommand cmd) {
        cmd.setStatus(AsyncCommand.SUCCESS);
        cmd.setClaimToken(null);
        mapper.updateState(cmd);
        // task-digest：异步命令成功摘要（执行是否成功 + 关键信息）
        com.xp.starsite.common.log.DigestLoggers.emit(com.xp.starsite.common.log.DigestLoggers.TASK,
                com.xp.starsite.common.log.DigestLoggers.fields(
                        "kind", "async_cmd", "cmdType", cmd.getCmdType(),
                        "cmdId", cmd.getId(), "status", "success"));
    }

    /** 失败重试：指数退避（60s·2^n，封顶 1h）；超限置 DEAD（死信） */
    public void markRetryOrFail(AsyncCommand cmd, Exception cause) {
        int retry = cmd.getRetryCount() == null ? 0 : cmd.getRetryCount();
        String msg = cause == null ? "" : cause.getMessage();
        cmd.setClaimToken(null);
        if (retry >= (cmd.getMaxRetry() == null ? 5 : cmd.getMaxRetry())) {
            cmd.setStatus(AsyncCommand.DEAD);
            cmd.setLastError(truncate(msg));
            mapper.updateState(cmd);
            com.xp.starsite.common.log.DigestLoggers.emit(com.xp.starsite.common.log.DigestLoggers.TASK,
                    com.xp.starsite.common.log.DigestLoggers.fields(
                            "kind", "async_cmd", "cmdType", cmd.getCmdType(),
                            "cmdId", cmd.getId(), "status", "dead",
                            "retry", retry, "error", msg));
            log.error("async cmd DEAD: id={}, type={}, lastError={}", cmd.getId(), cmd.getCmdType(), msg);
            return;
        }
        cmd.setRetryCount(retry + 1);
        long backoffSeconds = Math.min(60L * (1L << Math.min(retry, 6)), 3600L);
        cmd.setStatus(AsyncCommand.PENDING);
        cmd.setNextRunAt(LocalDateTime.now().plusSeconds(backoffSeconds));
        cmd.setLastError(truncate(msg));
        mapper.updateState(cmd);
        // task-digest：重试摘要（指数退避可视）
        com.xp.starsite.common.log.DigestLoggers.emit(com.xp.starsite.common.log.DigestLoggers.TASK,
                com.xp.starsite.common.log.DigestLoggers.fields(
                        "kind", "async_cmd", "cmdType", cmd.getCmdType(),
                        "cmdId", cmd.getId(), "status", "retry",
                        "retry", retry + 1, "backoffSeconds", backoffSeconds, "error", msg));
        log.warn("async cmd retry #{}: id={}, type={}, backoff={}s, err={}",
                retry + 1, cmd.getId(), cmd.getCmdType(), backoffSeconds, msg);
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 480 ? s.substring(0, 480) : s;
    }
}
