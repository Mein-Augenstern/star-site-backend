package com.xp.starsite.common.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 事件信封：所有跨服务事件的统一外壳（线上契约）。
 * eventId 全局唯一（UUID），消费端以 (eventId, consumerGroup) 做幂等。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope implements Serializable {

    /** 事件唯一 id（UUID） */
    private String eventId;

    /** 事件类型，见 EventTypes */
    private String eventType;

    /** 发出事件的服务名（spring.application.name） */
    private String sourceService;

    /** 业务类型，如 policy / report / family_member */
    private String bizType;

    /** 业务主键 */
    private Long bizId;

    /** 事件发生时间 */
    private LocalDateTime occurredAt;

    /** 事件体 JSON（具体领域事件类的序列化结果） */
    private String payloadJson;

    /** 发送方 traceId（链路透传：消费端还原后，发送/消费日志同链路可查；旧消息可空） */
    private String traceId;
}
