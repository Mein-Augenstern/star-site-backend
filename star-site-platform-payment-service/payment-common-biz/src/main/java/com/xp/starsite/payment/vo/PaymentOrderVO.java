package com.xp.starsite.payment.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单视图（含支付凭证，创建接口返回）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class PaymentOrderVO {

    private Long id;

    /** 支付单号 */
    private String orderNo;

    private String source;

    private String bizType;

    private Long bizId;

    /** 归属账号 id（支付人） */
    private Long ownerMemberId;

    /** 支付渠道 */
    private String channel;

    private String subject;

    private BigDecimal amount;

    /** 状态：1已创建 2成功 3已关闭 4已退款 */
    private Integer status;

    /** 状态文案 */
    private String statusText;

    /** 支付凭证（仅已创建状态返回；前端据此拉起支付/渲染收款码） */
    private String chargePayload;

    private String channelTradeNo;

    private LocalDateTime paidAt;

    private String remark;

    private LocalDateTime createdAt;
}
