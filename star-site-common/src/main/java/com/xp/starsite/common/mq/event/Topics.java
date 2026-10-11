package com.xp.starsite.common.mq.event;

/**
 * Topic 规划：按消费方划分（消费方负责订阅与幂等），
 * 生产方只管往 topic 发事件，不感知谁在消费。
 * 领域事件载荷定义在 star-site-api（如 event.reminder 包）。
 */
public final class Topics {

    private Topics() {
    }

    /** 消费方：reminder-service。提醒规则的注册/更新/撤销 */
    public static final String TOPIC_REMINDER = "TOPIC_REMINDER";

    /** 消费方：reminder-service。到期实例经延迟消息精确投递 */
    public static final String TOPIC_NOTIFICATION = "TOPIC_NOTIFICATION";

    /** 预留：文件上传完成 → 异步加工（PDF 解析、缩略图等） */
    public static final String TOPIC_FILE = "TOPIC_FILE";

    /** 预留：报告/指标变更 → AI 解读、年报等衍生加工 */
    public static final String TOPIC_REPORT = "TOPIC_REPORT";
}
