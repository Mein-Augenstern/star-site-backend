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
 * DIGEST_RPC_CLIENT  → logs/{app}/rpc-client-digest.log   （本服务作为客户端调内部服务）
 * DIGEST_RPC_SERVER  → logs/{app}/rpc-server-digest.log   （本服务作为服务端被调）
 * DIGEST_MQ_SEND     → logs/{app}/mq-send-digest.log      （消息发送摘要）
 * DIGEST_MQ_CONSUME  → logs/{app}/mq-consume-digest.log   （消息消费摘要）
 * DIGEST_DB          → logs/{app}/db-digest.log           （SQL 执行摘要）
 * DIGEST_TASK        → logs/{app}/task-digest.log         （定时任务/异步命令摘要）
 * DIGEST_EXTERNAL    → logs/{app}/external-digest.log     （外部 HTTP 调用摘要，如 LLM）
 * 另有 app-default.log：全量业务日志落盘（root logger，非 digest）
 * </pre>
 * 每行一个 JSON 对象，字段统一含 time/traceId/app/success/costMs（SOFA 摘要结构：
 * 执行是否成功、执行耗时、关键信息），超长值自动截断防磁盘膨胀；机器可解析。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class DigestLoggers {

    /** 本服务作为客户端发起内部调用（Feign 出站） */
    public static final Logger RPC_CLIENT = LoggerFactory.getLogger("DIGEST_RPC_CLIENT");
    /** 本服务作为服务端被调用（HTTP 入站） */
    public static final Logger RPC_SERVER = LoggerFactory.getLogger("DIGEST_RPC_SERVER");
    /** 消息发送 */
    public static final Logger MQ_SEND = LoggerFactory.getLogger("DIGEST_MQ_SEND");
    /** 消息消费 */
    public static final Logger MQ_CONSUME = LoggerFactory.getLogger("DIGEST_MQ_CONSUME");
    /** 数据库访问 */
    public static final Logger DB = LoggerFactory.getLogger("DIGEST_DB");
    /** 定时任务/异步命令 */
    public static final Logger TASK = LoggerFactory.getLogger("DIGEST_TASK");
    /** 外部 HTTP 调用（LLM 等第三方 API） */
    public static final Logger EXTERNAL = LoggerFactory.getLogger("DIGEST_EXTERNAL");

    /** 单字段值截断上限：digest 只留关键信息，大结构体会刷爆磁盘 */
    private static final int MAX_VALUE_LENGTH = 256;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DigestLoggers() {
    }

    /**
     * 输出一行 digest JSON。公共字段（time/traceId/app）自动补齐，调用方只放差异字段。
     * 统一字段约定：status（执行是否成功：success/biz_error/error/duplicate...）、
     * costMs（执行耗时 ms）、action/topic/target 等（关键信息）；超长值截断加省略标记。
     *
     * @param fields 业务字段：如 "topic"→"TOPIC_FILE"、"status"→"success"
     */
    public static void emit(Logger digestLogger, Map<String, Object> fields) {
        try {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("time", java.time.LocalDateTime.now().toString());
            line.put("traceId", TraceContext.current());
            line.put("app", APP_NAME);
            fields.forEach((k, v) -> line.put(k, truncate(v)));
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

    /** 值级截断：字符串超 256 字符截断加长度标记，防大结构体刷爆日志磁盘 */
    private static Object truncate(Object v) {
        if (v instanceof String s && s.length() > MAX_VALUE_LENGTH) {
            return s.substring(0, MAX_VALUE_LENGTH) + "...(" + s.length() + "chars)";
        }
        return v;
    }
}
