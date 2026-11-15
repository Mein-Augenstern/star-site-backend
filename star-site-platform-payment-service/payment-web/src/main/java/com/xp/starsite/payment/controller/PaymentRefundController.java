package com.xp.starsite.payment.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.lock.client.LockClient;
import com.xp.starsite.payment.controller.actions.PaymentActions;
import com.xp.starsite.payment.dto.RefundCreateDTO;
import com.xp.starsite.payment.service.PaymentRefundService;
import com.xp.starsite.payment.vo.PaymentRefundVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 退款接口：对已支付订单发起退款/查退款单。
 * 鉴权与模板约定同 {@link PaymentOrderController}。
 * <p>
 * 并发防线（2026-11 starsite-lock）：同支付单退款申请经 {@code payment-refund:{orderId}}
 * 锁串行化——并发双击串行后校验兜底；锁在控制器层获取/释放（service.apply 是
 * {@code @Transactional}，锁在事务内 finally 释放会留出「事务提交前窗口」，
 * 并发方可在窗口期读到未提交的旧状态绕过校验）。完整防顺序级重复提交需前端
 * 传退款请求号（届时可换 @StarsiteIdempotent 声明式幂等）。
 *
 * @author muyi
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentRefundController {

    private final PaymentRefundService refundService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;
    private final LockClient lockClient;

    /**
     * 发起退款（金额不传=全额退；全额退成功支付单流转已退款）
     *
     * @param memberId 当前登录账号（JWT 注入，操作人）
     * @param dto      退款内容
     */
    @PostMapping("/refunds")
    public R<PaymentRefundVO> apply(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                    @Valid @RequestBody RefundCreateDTO dto) {
        String lock = "payment-refund:" + dto.getOrderId();
        if (!lockClient.tryLock(lock)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "该订单退款处理中，请勿重复提交");
        }
        try {
            return writeTemplate.write(PaymentActions.REFUND_APPLY,
                    () -> refundService.apply(memberId, dto));
        } finally {
            lockClient.unlock(lock);
        }
    }

    /**
     * 退款单详情（仅操作人）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param id       退款单 id
     */
    @GetMapping("/refunds/{id}")
    public R<PaymentRefundVO> get(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                  @PathVariable("id") Long id) {
        return queryTemplate.query(PaymentActions.REFUND_GET,
                () -> refundService.get(memberId, id));
    }
}
