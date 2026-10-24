package com.xp.starsite.payment.channel;

import com.xp.starsite.payment.constant.PaymentChannelType;

import java.util.Map;

/**
 * 支付渠道端口（六边形端口，实现在 payment-integration 的渠道适配器）。
 * 统一收银台只面向本端口编程：预下单拿凭证 → 渠道异步回调验签解析 → 关单/退款。
 * 适配器清单：mock（沙箱，默认启用）、alipay（官方 SDK）、wechat（官方 SDK v3）。
 *
 * @author muyi
 * @since 2026-10
 */
public interface PaymentChannel {

    /** 渠道类型标识（对齐 {@link PaymentChannelType}，渠道路由键） */
    String channelType();

    /**
     * 预下单：向渠道申请收款，返回前端拉起支付所需的凭证
     * （mock=沙箱 token 串；alipay=二维码内容 qrcode；wechat=code_url）
     */
    ChargeResult charge(ChargeRequest request);

    /**
     * 验签并解析渠道异步回调
     *
     * @param headers 回调请求头（微信 v3 的签名四件套在此；alipay/mock 忽略）
     * @param rawBody 回调原始报文（alipay 为 form-urlencoded，wechat/mock 为 JSON）
     * @return 解析结果（含支付单号/渠道单号/支付金额）；验签失败抛
     *         {@link com.xp.starsite.common.exception.BizException}（401 语义，调用方拒绝响应）
     */
    CallbackResult verifyAndParse(Map<String, String> headers, String rawBody);

    /** 关闭未支付订单（用户放弃/超时） */
    void close(String orderNo);

    /** 渠道退款；返回渠道受理结果（异步到账以回调/查询为准） */
    RefundResult refund(RefundRequest request);
}
