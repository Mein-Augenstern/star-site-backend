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

    private Long id;

    private String refundNo;

    private Long orderId;

    private String orderNo;

    private String channel;

    private BigDecimal amount;

    /** 状态：1已受理 2成功 3失败 */
    private Integer status;

    /** 状态文案 */
    private String statusText;

    private String reason;

    private String channelRefundNo;

    private LocalDateTime refundedAt;

    private LocalDateTime createdAt;
}
