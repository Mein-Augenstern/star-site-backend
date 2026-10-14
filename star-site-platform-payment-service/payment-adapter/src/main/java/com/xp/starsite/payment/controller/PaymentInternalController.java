package com.xp.starsite.payment.controller;

import com.xp.starsite.api.client.PaymentOrderCreateInternalDTO;
import com.xp.starsite.api.dto.PaymentOrderDTO;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.payment.controller.actions.PaymentActions;
import com.xp.starsite.payment.dto.PaymentOrderCreateDTO;
import com.xp.starsite.payment.service.PaymentOrderService;
import com.xp.starsite.payment.vo.PaymentOrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务间内部接口（Feign 提供方）：业务服务（保险代收/会员订阅）的收银台入口。
 * <p>
 * /internal/** 不在网关路由表里，外网请求到网关直接 404；
 * JwtAuthInterceptor 只拦 /api/**，本控制器不经 JWT，仅限内网服务间调用。
 * 调用方统一走 payment-facade 的 PaymentInternalApi。
 * 业务逻辑统一经模板包裹（动作名 payment.internal.*）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/internal/payment")
@RequiredArgsConstructor
public class PaymentInternalController {

    private final PaymentOrderService orderService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 业务代下单（同业务键在途单复用返回，已支付报 400）
     *
     * @param memberId 支付人账号（归属校验主体）
     * @param dto      下单内容（source 必须为调用方自身服务名）
     */
    @PostMapping("/orders")
    public R<PaymentOrderDTO> createOrder(@RequestParam("memberId") Long memberId,
                                          @Valid @RequestBody PaymentOrderCreateInternalDTO dto) {
        return writeTemplate.write(PaymentActions.INTERNAL_ORDER_CREATE,
                () -> toDto(orderService.create(memberId, toCreateDTO(dto))));
    }

    /**
     * 按业务幂等键查询支付单（尚无支付单时 data=null）
     *
     * @param memberId 支付人账号（归属收口）
     */
    @GetMapping("/orders/by-biz")
    public R<PaymentOrderDTO> getByBiz(@RequestParam("memberId") Long memberId,
                                       @RequestParam("source") String source,
                                       @RequestParam("bizType") String bizType,
                                       @RequestParam("bizId") Long bizId) {
        return queryTemplate.query(PaymentActions.INTERNAL_ORDER_GET_BY_BIZ,
                () -> {
                    PaymentOrderVO vo = orderService.getByBiz(memberId, source, bizType, bizId);
                    return vo == null ? null : toDto(vo);
                });
    }

    private PaymentOrderCreateDTO toCreateDTO(PaymentOrderCreateInternalDTO internal) {
        PaymentOrderCreateDTO dto = new PaymentOrderCreateDTO();
        dto.setSource(internal.getSource());
        dto.setBizType(internal.getBizType());
        dto.setBizId(internal.getBizId());
        dto.setFamilyId(internal.getFamilyId());
        dto.setChannel(internal.getChannel());
        dto.setSubject(internal.getSubject());
        dto.setAmount(internal.getAmount());
        dto.setRemark(internal.getRemark());
        return dto;
    }

    /** VO → facade DTO：跨服务契约只透传快照字段 */
    private PaymentOrderDTO toDto(PaymentOrderVO vo) {
        PaymentOrderDTO dto = new PaymentOrderDTO();
        dto.setId(vo.getId());
        dto.setOrderNo(vo.getOrderNo());
        dto.setSource(vo.getSource());
        dto.setBizType(vo.getBizType());
        dto.setBizId(vo.getBizId());
        dto.setOwnerMemberId(vo.getOwnerMemberId());
        dto.setChannel(vo.getChannel());
        dto.setSubject(vo.getSubject());
        dto.setAmount(vo.getAmount());
        dto.setStatus(vo.getStatus());
        dto.setChargePayload(vo.getChargePayload());
        dto.setChannelTradeNo(vo.getChannelTradeNo());
        dto.setPaidAt(vo.getPaidAt());
        dto.setCreatedAt(vo.getCreatedAt());
        return dto;
    }
}
