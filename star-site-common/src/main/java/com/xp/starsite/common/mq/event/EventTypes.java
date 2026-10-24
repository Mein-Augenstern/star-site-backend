package com.xp.starsite.common.mq.event;

/**
 * 事件类型常量（一个类型 = 一种语义；tag 同名，消费端可按 tag 订阅）
 *
 * @author muyi
 * @since 2026-10
 */
public final class EventTypes {

    private EventTypes() {
    }

    /** 注册/更新提醒规则（全量 upsert 语义，幂等键 source+bizType+bizId） */
    public static final String REMINDER_REGISTER = "ReminderRegister";

    /** 撤销提醒规则（按幂等键删除） */
    public static final String REMINDER_CANCEL = "ReminderCancel";

    /** 到期实例投递（延迟消息，payload = occurrenceId） */
    public static final String OCCURRENCE_DUE = "OccurrenceDue";

    /** 文件上传完成（TOPIC_FILE，消费方按 bizType 过滤） */
    public static final String FILE_UPLOADED = "FileUploaded";

    /** 支付成功（TOPIC_PAYMENT，payload = PaymentSucceededEvent，消费方按 source 幂等更新业务单） */
    public static final String PAYMENT_SUCCEEDED = "PaymentSucceeded";
}
