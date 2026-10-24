package com.xp.starsite.payment.integration.channel;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeCloseModel;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝渠道适配器（官方 alipay-sdk-java）：当面付扫码（precreate）为主路径，
 * 异步回调 form 报文 rsaCheckV1 验签，退款走 alipay.trade.refund 同步受理。
 * 请求参数一律用 SDK 的 *Model 对象（setBizModel）——军规禁魔法串，且手拼
 * bizContent JSON 对标题/原因里的特殊字符天然脆弱。
 * 配置齐 star.payment.alipay.* 才装配（app-id/private-key/alipay-public-key），
 * 网关默认正式环境，沙箱经 star.payment.alipay.gateway-url 覆盖。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "star.payment.alipay", name = "app-id")
public class AlipayPaymentChannel implements PaymentChannel {

    private static final String CHARSET = "UTF-8";
    private static final String SIGN_TYPE = "RSA2";
    private static final String TRADE_SUCCESS = "TRADE_SUCCESS";
    private static final String TRADE_FINISHED = "TRADE_FINISHED";

    private final AlipayClient alipayClient;
    private final String alipayPublicKey;
    private final String notifyUrl;

    public AlipayPaymentChannel(@Value("${star.payment.alipay.app-id}") String appId,
                                @Value("${star.payment.alipay.private-key}") String privateKey,
                                @Value("${star.payment.alipay.alipay-public-key}") String alipayPublicKey,
                                @Value("${star.payment.alipay.gateway-url:https://openapi.alipay.com/gateway}") String gatewayUrl,
                                @Value("${star.payment.notify-url}") String notifyUrlBase) {
        this.alipayClient = new DefaultAlipayClient(gatewayUrl, appId, privateKey, "json", CHARSET, alipayPublicKey, SIGN_TYPE);
        this.alipayPublicKey = alipayPublicKey;
        this.notifyUrl = notifyUrlBase + "/" + PaymentChannelType.ALIPAY;
    }

    @Override
    public String channelType() {
        return PaymentChannelType.ALIPAY;
    }

    @Override
    public ChargeResult charge(ChargeRequest request) {
        AlipayTradePrecreateRequest alipayRequest = new AlipayTradePrecreateRequest();
        alipayRequest.setNotifyUrl(notifyUrl);
        AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
        model.setOutTradeNo(request.getOrderNo());
        model.setTotalAmount(request.getAmount().toPlainString());
        model.setSubject(request.getSubject());
        alipayRequest.setBizModel(model);
        try {
            AlipayTradePrecreateResponse response = alipayClient.execute(alipayRequest);
            if (!response.isSuccess()) {
                throw new BizException(500, "支付宝下单失败：" + response.getSubMsg());
            }
            // 凭证 = 二维码内容（前端渲染收款码）
            return new ChargeResult(response.getQrCode());
        } catch (AlipayApiException e) {
            log.error("支付宝预下单异常: orderNo={}", request.getOrderNo(), e);
            throw new BizException(500, "支付宝渠道暂不可用，请稍后再试");
        }
    }

    @Override
    public CallbackResult verifyAndParse(Map<String, String> headers, String rawBody) {
        Map<String, String> params = parseForm(rawBody);
        try {
            // rsaCheckV1 内部剔除 sign/sign_type 后验签；验签失败直接拒绝响应
            if (!AlipaySignature.rsaCheckV1(params, alipayPublicKey, CHARSET, SIGN_TYPE)) {
                log.warn("支付宝回调验签失败: trade_no={}", params.get("trade_no"));
                throw new BizException(401, "支付宝回调验签失败");
            }
        } catch (AlipayApiException e) {
            throw new BizException(401, "支付宝回调验签异常");
        }
        String tradeStatus = params.get("trade_status");
        boolean success = TRADE_SUCCESS.equals(tradeStatus) || TRADE_FINISHED.equals(tradeStatus);
        return new CallbackResult(params.get("out_trade_no"), params.get("trade_no"),
                new BigDecimal(params.getOrDefault("total_amount", "0")), success);
    }

    @Override
    public void close(String orderNo) {
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        AlipayTradeCloseModel model = new AlipayTradeCloseModel();
        model.setOutTradeNo(orderNo);
        request.setBizModel(model);
        try {
            AlipayTradeCloseResponse response = alipayClient.execute(request);
            log.info("支付宝关单: orderNo={}, success={}, msg={}", orderNo, response.isSuccess(), response.getSubMsg());
        } catch (AlipayApiException e) {
            // 关单失败不阻断本地关单（渠道侧超时自动关）
            log.warn("支付宝关单异常（不阻断）: orderNo={}, err={}", orderNo, e.getMessage());
        }
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        AlipayTradeRefundRequest alipayRequest = new AlipayTradeRefundRequest();
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(request.getOrderNo());
        model.setRefundAmount(request.getAmount().toPlainString());
        model.setOutRequestNo(request.getRefundNo());
        model.setRefundReason(request.getReason());
        alipayRequest.setBizModel(model);
        try {
            AlipayTradeRefundResponse response = alipayClient.execute(alipayRequest);
            if (!response.isSuccess()) {
                return new RefundResult(false, null, response.getSubMsg());
            }
            // 支付宝同步退款即最终态，fund_change=Y 表示资金变动成功
            boolean refunded = "Y".equals(response.getFundChange());
            return new RefundResult(refunded, response.getTradeNo(), refunded ? null : "fund_change=N，请查询确认");
        } catch (AlipayApiException e) {
            log.error("支付宝退款异常: refundNo={}", request.getRefundNo(), e);
            return new RefundResult(false, null, "支付宝渠道退款异常");
        }
    }

    /** form-urlencoded → 参数表（alipay 回调标准形态） */
    private Map<String, String> parseForm(String rawBody) {
        Map<String, String> params = new HashMap<>();
        for (String pair : rawBody.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }
}
