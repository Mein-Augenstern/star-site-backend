package com.xp.starsite.payment.integration.channel;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.payment.channel.PaymentChannel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 支付渠道路由注册表：汇聚容器内全部 {@link PaymentChannel} 适配器（各按配置条件装配），
 * 收银台按渠道类型取用。未装配的渠道（如生产关掉 mock、未配支付宝密钥）取用时即报
 * 「不支持的支付渠道」，把配置问题拦在调用点。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
public class PaymentChannelRegistry {

    private final Map<String, PaymentChannel> channels;

    public PaymentChannelRegistry(List<PaymentChannel> channelBeans) {
        this.channels = channelBeans.stream()
                .collect(Collectors.toMap(PaymentChannel::channelType, Function.identity()));
    }

    /** 按渠道路由键取适配器，未装配抛 400 */
    public PaymentChannel get(String channelType) {
        PaymentChannel channel = channels.get(channelType);
        if (channel == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "不支持的支付渠道：" + channelType);
        }
        return channel;
    }

    /** 当前已装配渠道清单（自检/运营可见） */
    public List<String> availableChannels() {
        return List.copyOf(channels.keySet());
    }
}
