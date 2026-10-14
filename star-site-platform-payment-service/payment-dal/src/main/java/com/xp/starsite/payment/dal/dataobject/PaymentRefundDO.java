package com.xp.starsite.payment.dal.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单数据对象（payment_refund 表镜像，DAL 自持）。
 * 领域语义见 core-model 的 entity/PaymentRefund。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class PaymentRefundDO {

    /** 主键（雪花 id，应用侧赋值） */
    private Long id;

    /** 退款单号（uk） */
    private String refundNo;

    /** 支付单 id */
    private Long orderId;

    /** 支付单号（冗余） */
    private String orderNo;

    /** 支付渠道（冗余） */
    private String channel;

    /** 渠道侧原交易号 */
    private String channelTradeNo;

    /** 渠道侧退款单号 */
    private String channelRefundNo;

    /** 退款金额（元） */
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
