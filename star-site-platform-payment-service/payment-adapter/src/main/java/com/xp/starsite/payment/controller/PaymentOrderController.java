package com.xp.starsite.payment.controller;

import com.xp.starsite.common.constants.AuthConstants;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 支付单接口（C 端收银台）：下单 → 拉起支付 → 查单 → 关单。
 * 鉴权：JwtAuthInterceptor 拦 /api/**（渠道回调走 /api/payment/notify/**，已排除）；
 * 业务逻辑统一经模板包裹（动作名 payment.order.*）。
 *
 * @author muyi
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentOrderController {

    private final PaymentOrderService orderService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 创建支付单（同业务键在途单复用；返回支付凭证供前端拉起支付）
     *
     * @param memberId 当前登录账号（JWT 注入，支付人）
     * @param dto      下单内容
     */
    @PostMapping("/orders")
    public R<PaymentOrderVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                    @Valid @RequestBody PaymentOrderCreateDTO dto) {
        return writeTemplate.write(PaymentActions.ORDER_CREATE,
                () -> orderService.create(memberId, dto));
    }

    /**
     * 支付单详情（仅归属人）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param id       支付单 id
     */
    @GetMapping("/orders/{id}")
    public R<PaymentOrderVO> get(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                 @PathVariable("id") Long id) {
        return queryTemplate.query(PaymentActions.ORDER_GET,
                () -> orderService.get(memberId, id));
    }

    /**
     * 支付单列表（归属账号范围，可按业务键过滤）
     *
     * @param memberId 当前登录账号（JWT 注入）
     */
    @GetMapping("/orders")
    public R<List<PaymentOrderVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                        @RequestParam(value = "source", required = false) String source,
                                        @RequestParam(value = "bizType", required = false) String bizType,
                                        @RequestParam(value = "bizId", required = false) Long bizId) {
        return queryTemplate.query(PaymentActions.ORDER_LIST,
                () -> orderService.list(memberId, source, bizType, bizId));
    }

    /**
     * 按业务幂等键查单（业务方驱动状态流转，如「保费已缴」）
     *
     * @param memberId 当前登录账号（JWT 注入）
     */
    @GetMapping("/orders/by-biz")
    public R<PaymentOrderVO> getByBiz(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                      @RequestParam("source") String source,
                                      @RequestParam("bizType") String bizType,
                                      @RequestParam("bizId") Long bizId) {
        return queryTemplate.query(PaymentActions.ORDER_GET_BY_BIZ,
                () -> orderService.getByBiz(memberId, source, bizType, bizId));
    }

    /**
     * 关闭在途支付单（用户放弃支付）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param id       支付单 id
     */
    @PostMapping("/orders/{id}/close")
    public R<Void> close(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                         @PathVariable("id") Long id) {
        return writeTemplate.write(PaymentActions.ORDER_CLOSE,
                () -> orderService.close(memberId, id));
    }
}
