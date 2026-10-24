package com.xp.starsite.payment.integration.channel;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.payment.channel.CallbackResult;
import com.xp.starsite.payment.channel.ChargeRequest;
import com.xp.starsite.payment.channel.ChargeResult;
import com.xp.starsite.payment.channel.PaymentChannel;
import com.xp.starsite.payment.channel.RefundRequest;
import com.xp.starsite.payment.channel.RefundResult;
import com.xp.starsite.payment.constant.PaymentChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

/**
 * mock 沙箱渠道（默认启用）：完整走「预下单凭证 → 签名回调 → 验签入账」真实流程，
 * 只把渠道侧换成 HMAC-SHA256 本地签名——联调/演示零外部依赖，生产经
 * star.payment.mock.enabled=false 关闭（关闭后创建 mock 渠道支付单即报不支持渠道）。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "star.payment.mock.enabled", havingValue = "true", matchIfMissing = true)
public class MockPaymentChannel implements PaymentChannel {

    /** 回调报文字段（JSON） */
    public static final String FIELD_ORDER_NO = "orderNo";
    public static final String FIELD_TRADE_NO = "tradeNo";
    public static final String FIELD_PAID_AMOUNT = "paidAmount";
    public static final String FIELD_SIGN = "sign";

    private final String secret;

    public MockPaymentChannel(@Value("${star.payment.mock.secret:star-site-mock-payment-secret-dev-only}") String secret) {
        this.secret = secret;
    }

    @Override
    public String channelType() {
        return PaymentChannelType.MOCK;
    }

    @Override
    public ChargeResult charge(ChargeRequest request) {
        // 凭证 = 对单号+金额的签名串；模拟支付端（/mock/pay）按同密钥回签形成闭环
        String payload = request.getOrderNo() + "|" + request.getAmount().toPlainString() + "|" + sign(request.getOrderNo());
        return new ChargeResult("mockpay://" + payload);
    }

    @Override
    public CallbackResult verifyAndParse(Map<String, String> headers, String rawBody) {
        String orderNo = jsonField(rawBody, FIELD_ORDER_NO);
        String tradeNo = jsonField(rawBody, FIELD_TRADE_NO);
        String amountText = jsonField(rawBody, FIELD_PAID_AMOUNT);
        String sign = jsonField(rawBody, FIELD_SIGN);
        if (orderNo == null || tradeNo == null || amountText == null || sign == null) {
            throw new BizException(400, "mock 回调报文缺字段");
        }
        if (!sign(orderNo).equals(sign)) {
            log.warn("mock 回调验签失败: orderNo={}", orderNo);
            throw new BizException(401, "mock 回调验签失败");
        }
        return new CallbackResult(orderNo, tradeNo, new BigDecimal(amountText), true);
    }

    @Override
    public void close(String orderNo) {
        // mock 渠道无外部状态，关单即本地语义
        log.info("mock 渠道关单: orderNo={}", orderNo);
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        return new RefundResult(true, "MOCKRF" + Math.abs(request.getRefundNo().hashCode()), null);
    }

    /** 沙箱签名：HMAC-SHA256(secret, orderNo)，模拟支付端与回调共用 */
    public String sign(String orderNo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(orderNo.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("mock 渠道签名失败", e);
        }
    }

    /** 极简 JSON 字段提取（沙箱报文字段全是平铺字符串/数字，不值得引 JSON 库） */
    private String jsonField(String json, String field) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"" + field + "\"\\s*:\\s*\"?([^\",}]+)\"?").matcher(json);
        return m.find() ? m.group(1).trim() : null;
    }
}
