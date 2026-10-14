package com.xp.starsite.payment.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单（payment_refund）：对支付成功支付单的部分/全额退款 attempt。
 * 幂等约束：uk_refund_no（应用侧退款单号）、uk_channel_refund（channel+channel_refund_no）防渠道重放。
 * 纯 POJO——表结构映射在 DAL 的 mapper XML。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class PaymentRefund {

    /** 退款已受理（渠道处理中） */
    public static final int STATUS_APPLIED = 1;

    /** 退款成功 */
    public static final int STATUS_SUCCESS = 2;

    /** 退款失败（终态，可重新发起） */
    public static final int STATUS_FAILED = 3;

    /** 雪花 id（应用侧赋值） */
    private Long id;

    /** 退款单号（RF 前缀 + 雪花；渠道侧 out_refund_no） */
    private String refundNo;

    /** 支付单 id */
    private Long orderId;

    /** 支付单号（冗余，免联表） */
    private String orderNo;

    /** 支付渠道（随支付单冗余） */
    private String channel;

    /** 渠道侧原交易号 */
    private String channelTradeNo;

    /** 渠道侧退款单号（回调回填） */
    private String channelRefundNo;

    /** 退款金额（元，≤ 支付单未退金额） */
    private BigDecimal amount;

    /** 状态：1已受理 2成功 3失败 */
    private Integer status;

    /** 退款原因 */
    private String reason;

    /** 操作账号 id */
    private Long operatorMemberId;

    /** 退款成功时间 */
    private LocalDateTime refundedAt;

    /** 逻辑删标志（0 正常 1 已删） */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
