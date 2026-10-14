package com.xp.starsite.payment.service;

import com.xp.starsite.payment.dto.RefundCreateDTO;
import com.xp.starsite.payment.vo.PaymentRefundVO;

/**
 * 退款服务：对支付成功支付单发起退款（渠道受理 → 状态机）。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface PaymentRefundService {

    /**
     * 发起退款（金额不传=全额退；退款成功且为全额时支付单流转已退款）
     *
     * @param memberId 当前登录账号（操作人）
     * @throws com.xp.starsite.common.exception.BizException 400 支付单不存在/状态不允许/金额超限；403 非本人
     */
    PaymentRefundVO apply(Long memberId, RefundCreateDTO dto);

    /**
     * 退款单详情（仅归属人可见）
     *
     * @throws com.xp.starsite.common.exception.BizException 400 不存在；403 非本人
     */
    PaymentRefundVO get(Long memberId, Long id);
}
