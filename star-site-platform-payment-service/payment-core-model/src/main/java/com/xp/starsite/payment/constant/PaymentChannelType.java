package com.xp.starsite.payment.constant;

/**
 * 支付渠道类型（payment_order.channel / 渠道路由键）。
 * 值随支付单落库且出现在渠道回调 URL（/api/payment/notify/{channel}），只增不改名。
 *
 * @author muyi
 * @since 2026-10
 */
public final class PaymentChannelType {

    /** mock 沙箱渠道（默认启用，联调/演示用；生产经 star.payment.mock.enabled=false 关闭） */
    public static final String MOCK = "mock";

    /** 支付宝（当面付扫码/手机网站支付，star.payment.alipay.* 配置启用） */
    public static final String ALIPAY = "alipay";

    /** 微信支付（Native 扫码，star.payment.wechat.* 配置启用） */
    public static final String WECHAT = "wechat";

    private PaymentChannelType() {
    }
}
