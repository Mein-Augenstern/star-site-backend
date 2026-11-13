package com.xp.starsite.payment.controller;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.payment.service.PaymentOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 渠道异步回调入口（外部渠道服务器调用，无 JWT）。
 * 双重放行：网关白名单 /api/payment/notify/** + 服务拦截器 excludePathPatterns；
 * 唯一防线是渠道验签（{@link PaymentOrderService#handleNotify}）。
 * 返回渠道原生应答体——不套业务 R 模板（模板约定：渠道协议/文件流响应例外）；
 * 验签失败/金额不符返回 4xx 原文，渠道侧会重试。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@RestController
@RequestMapping("/api/payment/notify")
@RequiredArgsConstructor
public class PaymentNotifyController {

    private final PaymentOrderService orderService;

    /**
     * 渠道异步回调（mock/alipay/wechat 统一入口，按路径段路由渠道适配器）
     *
     * @param channel 渠道路由键（mock/alipay/wechat）
     * @param headers 回调请求头（微信 v3 验签四件套）
     * @param rawBody 回调原始报文（alipay 为 form-urlencoded，其余 JSON）
     * @return 渠道原生应答体（成功 "success"/微信 JSON；失败 4xx 由渠道重试）
     */
    @PostMapping("/{channel}")
    public ResponseEntity<String> onNotify(@PathVariable("channel") String channel,
                                           @RequestHeader Map<String, String> headers,
                                           @RequestBody String rawBody) {
        try {
            String ack = orderService.handleNotify(channel, lowercaseKeys(headers), rawBody);
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(ack);
        } catch (BizException e) {
            // 验签失败/金额不符：4xx 原文应答，渠道按策略重试；本地状态机未动
            log.warn("渠道回调拒绝: channel={}, code={}, msg={}", channel, e.getCode(), e.getMessage());
            return ResponseEntity.status(e.getCode() == 401 ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(e.getMessage());
        }
    }

    /** 渠道签名头大小写不敏感（网关/HTTP 实现可能改写），统一转小写键 */
    private Map<String, String> lowercaseKeys(Map<String, String> headers) {
        Map<String, String> lowered = new HashMap<>();
        headers.forEach((k, v) -> lowered.put(k == null ? "" : k.toLowerCase(), v));
        return lowered;
    }
}
