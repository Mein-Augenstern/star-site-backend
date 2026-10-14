package com.xp.starsite.payment.channel;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 预下单请求（渠道无关）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@AllArgsConstructor
public class ChargeRequest {

    /** 支付单号（渠道 out_trade_no） */
    private String orderNo;

    /** 商品标题 */
    private String subject;

    /** 金额（元） */
    private BigDecimal amount;

    /** 回调通知地址（渠道服务器主动 POST 的入口） */
    private String notifyUrl;
}
