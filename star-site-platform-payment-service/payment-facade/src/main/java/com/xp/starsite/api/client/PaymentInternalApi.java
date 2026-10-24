package com.xp.starsite.api.client;

import com.xp.starsite.api.dto.PaymentOrderDTO;
import com.xp.starsite.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * payment 服务内部接口（Feign 契约）：业务服务（保险代收/会员订阅等）的收银台入口。
 * 服务端实现见 payment 服务 PaymentInternalController。
 *
 * /internal/** 不经网关路由（外网 404），仅限内网服务间调用。
 * 归属校验：payment 侧按 ownerMemberId 收口（调用方透传登录账号）。
 *
 * @author muyi
 * @since 2026-10
 */
@FeignClient(name = "star-site-platform-payment", contextId = "paymentInternalApi", path = "/internal/payment")
public interface PaymentInternalApi {

    /**
     * 创建支付单（业务代下单；同业务键在途单复用返回，已支付报 400）
     *
     * @param memberId 支付人账号（归属校验主体）
     * @param dto      下单内容（source 必须为调用方自身服务名，bizType/bizId 为业务单据）
     * @return 支付单快照（含支付凭证）
     */
    @PostMapping("/orders")
    R<PaymentOrderDTO> createOrder(@RequestParam("memberId") Long memberId,
                                   @RequestBody PaymentOrderCreateInternalDTO dto);

    /**
     * 按业务幂等键查询支付单（驱动业务单状态流转，如「保费已缴」）
     *
     * @return 支付单快照；尚无支付单时 data=null
     */
    @GetMapping("/orders/by-biz")
    R<PaymentOrderDTO> getByBiz(@RequestParam("memberId") Long memberId,
                                @RequestParam("source") String source,
                                @RequestParam("bizType") String bizType,
                                @RequestParam("bizId") Long bizId);
}
