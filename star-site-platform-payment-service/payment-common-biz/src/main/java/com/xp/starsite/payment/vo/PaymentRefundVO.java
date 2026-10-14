package com.xp.starsite.payment.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单视图
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class PaymentRefundVO {

    /** 雪花 id */
    private Long id;

    /** 退款单号 */
    private String refundNo;

    /** 支付单 id */
    private Long orderId;

    /** 支付单号（冗余） */
    private String orderNo;

    /** 支付渠道 */
    private String channel;

    /** 退款金额（元） */
    private BigDecimal amount;

    /** 状态：1已受理 2成功 3失败 */
    private Integer status;

    /** 状态文案 */
    private String statusText;

    /** 退款原因 */
    private String reason;

    /** 渠道侧退款单号 */
    private String channelRefundNo;

    /** 退款成功时间 */
    private LocalDateTime refundedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
