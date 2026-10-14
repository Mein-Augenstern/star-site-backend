package com.xp.starsite.payment.channel;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 渠道异步回调解析结果（已验签）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@AllArgsConstructor
public class CallbackResult {

    /** 支付单号（渠道报文里的 out_trade_no） */
    private String orderNo;

    /** 渠道侧交易号 */
    private String channelTradeNo;

    /** 渠道确认的支付金额（元） */
    private BigDecimal paidAmount;

    /** 渠道判定是否支付成功（false = 用户未支付/关闭类通知，调用方忽略或关单） */
    private boolean success;
}
