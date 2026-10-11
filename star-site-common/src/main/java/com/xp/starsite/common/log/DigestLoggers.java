package com.xp.starsite.common.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SOFA 式分文件 digest 日志出口：每个中间件视角一个独立 logger（对应独立日志文件）。
 * <p>
 * 文件映射由各服务 logback-spring.xml 定义，命名对齐 SOFA 惯例：
 * <pre>
 * DIGEST_RPC_CLIENT  → logs/{app}/rpc-client-digest.log   （本服务作为客户端调别人）
 * DIGEST_RPC_SERVER  → logs/{app}/rpc-server-digest.log   （本服务作为服务端被调）
 * DIGEST_MQ_SEND     → logs/{app}/mq-send-digest.log      （消息发送摘要）
 * DIGEST_MQ_CONSUME  → logs/{app}/mq-consume-digest.log   （消息消费摘要）
 * DIGEST_DB          → logs/{app}/db-digest.log           （SQL 执行摘要）
 * </pre>
 * 每行一个 JSON 对象（含 traceId），机器可解析；字段固定顺序便于肉眼比对。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class DigestLoggers {

    /** 本服务作为客户端发起调用（Feign 出站） */
    public static final Logger RPC_CLIENT = LoggerFactory.getLogger("DIGEST_RPC_CLIENT");
    /** 本服务作为服务端被调用（HTTP 入站） */
    public static final Logger RPC_SERVER = LoggerFactory.getLogger("DIGEST_RPC_SERVER");
    /** 消息发送 */
    public static final Logger MQ_SEND = LoggerFactory.getLogger("DIGEST_MQ_SEND");
    /** 消息消费 */
    public static final Logger MQ_CONSUME = LoggerFactory.getLogger("DIGEST_MQ_CONSUME");
    /** 数据库访问 */
    public static final Logger DB = LoggerFactory.getLogger("DIGEST_DB");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DigestLoggers() {
    }

    /**
     * 输出一行 digest JSON。公共字段（time/traceId/app）自动补齐，调用方只放差异字段。
     *
     * @param fields 业务字段：如 "topic"→"TOPIC_FILE"、"status"→"success"
     */
    public static void emit(Logger digestLogger, Map<String, Object> fields) {
        try {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("time", java.time.LocalDateTime.now().toString());
            line.put("traceId", TraceContext.current());
            line.put("app", APP_NAME);
            line.putAll(fields);
            digestLogger.info(MAPPER.writeValueAsString(line));
        } catch (Exception ignore) {
            // digest 日志绝不影响业务
        }
    }

    /** 应用名由 log config 注入（spring.application.name），未注入时空串 */
    public static String APP_NAME = "";

    /** 快捷构造字段 */
    public static Map<String, Object> fields(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }
}
