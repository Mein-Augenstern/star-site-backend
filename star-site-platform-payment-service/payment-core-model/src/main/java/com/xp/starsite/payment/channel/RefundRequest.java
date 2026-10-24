package com.xp.starsite.payment.channel;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 渠道退款请求
 *
 * @author muyi
 * @since 2026-10
 */
@Data
@AllArgsConstructor
public class RefundRequest {

    /** 支付单号 */
    private String orderNo;

    /** 渠道侧原交易号 */
    private String channelTradeNo;

    /** 退款单号（渠道 out_refund_no，幂等键） */
    private String refundNo;

    /** 退款金额（元） */
    private BigDecimal amount;

    /** 退款原因 */
    private String reason;
}
