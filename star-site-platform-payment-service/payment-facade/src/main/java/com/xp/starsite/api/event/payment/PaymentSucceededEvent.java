package com.xp.starsite.api.event.payment;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付成功领域事件（TOPIC_PAYMENT / PaymentSucceeded）：渠道回调确认入账后发布，
 * 业务方按 source+bizType+bizId 幂等驱动业务单状态流转（如保单「缴费中→已缴清」）。
 * MQ 停用期事件不投递，业务方以 PaymentInternalApi.getByBiz 同步查询兜底。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class PaymentSucceededEvent implements Serializable {

    /** 支付单号 */
    private String orderNo;

    /** 来源服务（业务归属，幂等键第一段） */
    private String source;

    /** 业务类型（幂等键第二段） */
    private String bizType;

    /** 业务主键（幂等键第三段） */
    private Long bizId;

    /** 归属账号 id（支付人） */
    private Long ownerMemberId;

    /** 支付渠道 */
    private String channel;

    /** 渠道侧交易号 */
    private String channelTradeNo;

    /** 实付金额（元） */
    private BigDecimal amount;

    /** 支付成功时间 */
    private LocalDateTime paidAt;
}
