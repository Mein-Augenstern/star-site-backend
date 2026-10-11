package com.xp.starsite.common.util;

import org.springframework.beans.factory.annotation.Value;

/**
 * 雪花 id 生成器（替代 MyBatis-Plus 的 ASSIGN_ID）：64 位 = 1 符号位 + 41 时间戳 + 10 机器位 + 12 序列位。
 * <p>
 * 使用约定：需要「insert 前拿到 id」的场景（事务消息事件载荷、跨服务引用）在业务层显式
 * {@code entity.setId(IdGenerator.nextId())}；纯自增表不适用（DB 自增）。
 * 机器位取自配置 {@code star.snowflake.worker-id}（多实例部署时各不相同即可，单机默认 1）。
 * 单例 Bean：{@code IdGenerator} 由 StarSiteLogConfig（或各服务配置）注册；静态场景可 {@code new IdGenerator(1)}。
 *
 * @author xiaopu
 * @since 2026-10
 */
public class IdGenerator {

    /** 起始时间戳（2026-01-01），41 位时间戳可用约 69 年 */
    private static final long EPOCH = 1767225600000L;

    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS;

    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public IdGenerator(@Value("${star.snowflake.worker-id:1}") long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("worker-id 必须在 0~" + MAX_WORKER_ID + " 之间: " + workerId);
        }
        this.workerId = workerId;
    }

    /** 生成下一个全局唯一 id（同一毫秒内序列递增，翻毫秒重置） */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            // 时钟回拨：等待追平（回拨秒级内可接受）
            timestamp = lastTimestamp;
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 序列耗尽，自旋到下一毫秒
                while ((timestamp = System.currentTimeMillis()) <= lastTimestamp) {
                    // busy wait
                }
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT) | (workerId << WORKER_ID_SHIFT) | sequence;
    }
}
