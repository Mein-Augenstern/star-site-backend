package com.xp.starsite.payment.integration.channel;

import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.CloseOrderRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.Status;
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

import java.math.BigDecimal;
import java.util.Map;

/**
 * 微信支付渠道适配器（官方 wechatpay-java，Native 扫码 + v3 回调验签解密 + 退款）。
 * 配置齐 star.payment.wechat.*（merchant-id/merchant-serial-no/private-key-path/api-v3-key/app-id）才装配；
 * 平台证书经 RSAAutoCertificateConfig 自动下载轮换（需外网可达微信支付）。
 * 回调验签依赖请求头四件套（Wechatpay-Serial/Nonce/Signature/Timestamp），
 * 由回调入口统一透传 headers。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "star.payment.wechat", name = {"merchant-id", "merchant-serial-no", "private-key-path", "api-v3-key", "app-id"})
public class WechatPayChannel implements PaymentChannel {

    private static final String HDR_SERIAL = "wechatpay-serial";
    private static final String HDR_NONCE = "wechatpay-nonce";
    private static final String HDR_SIGNATURE = "wechatpay-signature";
    private static final String HDR_TIMESTAMP = "wechatpay-timestamp";

    private final String merchantId;
    private final String appId;
    private final String notifyUrl;
    private final NativePayService nativePayService;
    private final NotificationParser notificationParser;
    private final com.wechat.pay.java.service.refund.RefundService refundService;

    public WechatPayChannel(@Value("${star.payment.wechat.merchant-id}") String merchantId,
                            @Value("${star.payment.wechat.merchant-serial-no}") String merchantSerialNo,
                            @Value("${star.payment.wechat.private-key-path}") String privateKeyPath,
                            @Value("${star.payment.wechat.api-v3-key}") String apiV3Key,
                            @Value("${star.payment.wechat.app-id}") String appId,
                            @Value("${star.payment.notify-url}") String notifyUrlBase) {
        RSAAutoCertificateConfig config = new RSAAutoCertificateConfig.Builder()
                .merchantId(merchantId)
                .privateKeyFromPath(privateKeyPath)
                .merchantSerialNumber(merchantSerialNo)
                .apiV3Key(apiV3Key)
                .build();
        this.merchantId = merchantId;
        this.appId = appId;
        this.notifyUrl = notifyUrlBase + "/" + PaymentChannelType.WECHAT;
        this.nativePayService = new NativePayService.Builder().config(config).build();
        this.notificationParser = new NotificationParser((NotificationConfig) config);
        this.refundService = new com.wechat.pay.java.service.refund.RefundService.Builder().config(config).build();
    }

    @Override
    public String channelType() {
        return PaymentChannelType.WECHAT;
    }

    @Override
    public ChargeResult charge(ChargeRequest request) {
        PrepayRequest prepay = new PrepayRequest();
        prepay.setAppid(appId);
        prepay.setMchid(merchantId);
        prepay.setDescription(request.getSubject());
        prepay.setOutTradeNo(request.getOrderNo());
        prepay.setNotifyUrl(notifyUrl);
        Amount amount = new Amount();
        amount.setTotal(toCents(request.getAmount()));
        prepay.setAmount(amount);
        try {
            PrepayResponse response = nativePayService.prepay(prepay);
            // 凭证 = code_url（前端渲染收款码）
            return new ChargeResult(response.getCodeUrl());
        } catch (Exception e) {
            log.error("微信预下单异常: orderNo={}", request.getOrderNo(), e);
            throw new BizException(500, "微信支付暂不可用，请稍后再试");
        }
    }

    @Override
    public CallbackResult verifyAndParse(Map<String, String> headers, String rawBody) {
        RequestParam param = new RequestParam.Builder()
                .serialNumber(headers.get(HDR_SERIAL))
                .nonce(headers.get(HDR_NONCE))
                .signature(headers.get(HDR_SIGNATURE))
                .timestamp(headers.get(HDR_TIMESTAMP))
                .body(rawBody)
                .build();
        Transaction transaction;
        try {
            // 验签 + AES-GCM 解密一体完成；失败抛 WechatPayException
            transaction = notificationParser.parse(param, Transaction.class);
        } catch (Exception e) {
            log.warn("微信回调验签失败: err={}", e.getMessage());
            throw new BizException(401, "微信回调验签失败");
        }
        boolean success = transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS;
        BigDecimal paid = transaction.getAmount() == null ? BigDecimal.ZERO
                : BigDecimal.valueOf(transaction.getAmount().getTotal(), 2);
        return new CallbackResult(transaction.getOutTradeNo(), transaction.getTransactionId(), paid, success);
    }

    @Override
    public void close(String orderNo) {
        try {
            CloseOrderRequest closeRequest = new CloseOrderRequest();
            closeRequest.setMchid(merchantId);
            closeRequest.setOutTradeNo(orderNo);
            nativePayService.closeOrder(closeRequest);
            log.info("微信关单: orderNo={}", orderNo);
        } catch (Exception e) {
            // 关单失败不阻断本地关单（渠道侧超时自动关）
            log.warn("微信关单异常（不阻断）: orderNo={}, err={}", orderNo, e.getMessage());
        }
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        CreateRequest refundRequest = new CreateRequest();
        refundRequest.setOutTradeNo(request.getOrderNo());
        refundRequest.setOutRefundNo(request.getRefundNo());
        refundRequest.setReason(request.getReason());
        AmountReq amount = new AmountReq();
        amount.setRefund((long) toCents(request.getAmount()));
        amount.setTotal((long) toCents(request.getAmount()));
        amount.setCurrency("CNY");
        refundRequest.setAmount(amount);
        try {
            Refund refund = refundService.create(refundRequest);
            // SUCCESS=退款成功；PROCESSING=处理中（到账以渠道退款通知为准，查询兜底）
            boolean done = refund.getStatus() == Status.SUCCESS;
            return new RefundResult(done, refund.getRefundId(), done ? null : "退款处理中，以渠道通知为准");
        } catch (Exception e) {
            log.error("微信退款异常: refundNo={}", request.getRefundNo(), e);
            return new RefundResult(false, null, "微信渠道退款异常");
        }
    }

    /** 元 → 分（微信金额单位为分） */
    private int toCents(BigDecimal yuan) {
        return yuan.movePointRight(2).intValueExact();
    }
}
