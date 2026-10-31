package com.xp.starsite.bench.controller;

import com.xp.starsite.asynccmd.client.AsyncCommandClient;
import com.xp.starsite.lock.client.LockClient;
import com.xp.starsite.sequence.facade.StarsiteIdGenerator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 三组件压测端点：每个端点对应一个组件的核心操作。
 * <p>
 * 压测方法：
 * <pre>
 * # 单线程基线
 * ab -n 2000 -c 1 http://localhost:18090/api/bench/id
 *
 * # 并发压测
 * ab -n 5000 -c 16 http://localhost:18090/api/bench/id
 *
 * # Python 并发脚本（更精确的百分位）
 * python3 scripts/bench.py --url http://localhost:18090 --scenario id --concurrency 16
 * </pre>
 *
 * @author muyi
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/bench")
public class BenchController {

    /** 发号器门面 */
    private final StarsiteIdGenerator idGenerator;

    /** 异步命令客户端 */
    private final AsyncCommandClient asyncClient;

    /** 分布式锁客户端 */
    private final LockClient lockClient;

    /**
     * 构造注入。
     */
    public BenchController(StarsiteIdGenerator idGenerator,
                           AsyncCommandClient asyncClient,
                           LockClient lockClient) {
        this.idGenerator = idGenerator;
        this.asyncClient = asyncClient;
        this.lockClient = lockClient;
    }

    /**
     * sequence 组件压测端点：生成一个雪花 ID。
     * <p>RPC 链路：HTTP → Spring MVC → StarsiteIdGenerator.next() → JSON
     *
     * @return 包含 id/idStr/workerId 的 JSON
     */
    @GetMapping("/id")
    public Map<String, Object> id() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", idGenerator.next());
        r.put("idStr", idGenerator.nextStr());
        r.put("workerId", idGenerator.getWorkerId());
        return r;
    }

    /**
     * lock 组件压测端点：获取锁 + 立即释放（一个完整周期）。
     * <p>RPC 链路：HTTP → Spring MVC → LockClient.tryLock → unlock → JSON
     *
     * @param name 锁名（默认 bench-lock）
     * @return 包含 lockName/acquired 的 JSON
     */
    @GetMapping("/lock")
    public Map<String, Object> lock(@RequestParam(required = false, defaultValue = "bench-lock") String name) {
        boolean acquired = lockClient.tryLock(name);
        if (acquired) {
            lockClient.unlock(name);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("lockName", name);
        r.put("acquired", acquired);
        return r;
    }

    /**
     * lock 组件压测端点：限时等待获取（测试等待语义下的吞吐）。
     *
     * @param name 锁名
     * @param waitMs 等待毫秒
     * @return JSON
     */
    @GetMapping("/lock-wait")
    public Map<String, Object> lockWait(@RequestParam(required = false, defaultValue = "bench-lock") String name,
                                        @RequestParam(required = false, defaultValue = "100") long waitMs) {
        boolean acquired = lockClient.tryLock(name, waitMs);
        if (acquired) {
            lockClient.unlock(name);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("lockName", name);
        r.put("waitMs", waitMs);
        r.put("acquired", acquired);
        return r;
    }

    /**
     * async-command 组件压测端点：提交一条异步命令。
     * <p>RPC 链路：HTTP → Spring MVC → AsyncCommandClient.submit → MySQL INSERT → JSON
     *
     * @param userId 用户 ID（作为 bizKey）
     * @param message 消息载荷
     * @return 包含 commandId/status 的 JSON
     */
    @PostMapping("/async")
    public Map<String, Object> async(@RequestParam(required = false, defaultValue = "bench-user") String userId,
                                     @RequestParam(required = false, defaultValue = "bench-msg") String message) {
        var cmd = asyncClient.submit("bench-notify", userId,
                "{\"message\":\"" + message + "\"}");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("commandId", cmd.getId());
        r.put("status", cmd.getStatus());
        return r;
    }

    /**
     * async-command 组件压测端点：幂等提交（测试防重复路径）。
     *
     * @param userId 用户 ID
     * @return JSON
     */
    @PostMapping("/async-idempotent")
    public Map<String, Object> asyncIdempotent(@RequestParam(required = false, defaultValue = "bench-user") String userId) {
        var cmd = asyncClient.submitIdempotent("bench-idempotent", userId, "{}");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("skipped", cmd == null);
        if (cmd != null) {
            r.put("commandId", cmd.getId());
        }
        return r;
    }

    /**
     * 健康检查 / 压测前置探测。
     *
     * @return 三组件配置概览
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("status", "UP");
        r.put("workerId", idGenerator.getWorkerId());
        r.put("allocator", idGenerator.describe());
        return r;
    }
}
