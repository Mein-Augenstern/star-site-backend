package com.xp.starsite.payment.service.impl;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.common.util.IdGenerator;
import com.xp.starsite.payment.channel.RefundRequest;
import com.xp.starsite.payment.channel.RefundResult;
import com.xp.starsite.payment.constant.PaymentChannelType;
import com.xp.starsite.payment.dto.RefundCreateDTO;
import com.xp.starsite.payment.entity.PaymentOrder;
import com.xp.starsite.payment.entity.PaymentRefund;
import com.xp.starsite.payment.integration.channel.PaymentChannelRegistry;
import com.xp.starsite.payment.repository.PaymentOrderRepository;
import com.xp.starsite.payment.repository.PaymentRefundRepository;
import com.xp.starsite.payment.service.PaymentRefundService;
import com.xp.starsite.payment.vo.PaymentRefundVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款服务实现：支付成功单方可退 → 金额上限校验（不传=全额）→ 渠道受理 → 状态机。
 * 单号先于渠道调用生成（refund_no 是渠道侧 out_refund_no 幂等键）。
 * 退款终态判定：mock/支付宝为同步终态（受理即成功），微信异步处理中以渠道通知为准
 * （受理成功先保持已受理，避免提前标记已退款）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRefundServiceImpl implements PaymentRefundService {

    private final PaymentOrderRepository orderRepository;
    private final PaymentRefundRepository refundRepository;
    private final PaymentChannelRegistry channelRegistry;
    private final IdGenerator idGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentRefundVO apply(Long memberId, RefundCreateDTO dto) {
        PaymentOrder order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new BizException(ResultCode.BAD_REQUEST.getCode(), "支付单不存在"));
        if (!order.getOwnerMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权限操作该支付单");
        }
        if (order.getStatus() != PaymentOrder.STATUS_SUCCESS) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "仅已支付的订单可退款");
        }
        if (order.getChannelTradeNo() == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "支付单缺少渠道交易号，无法退款");
        }
        BigDecimal amount = dto.getAmount() == null ? order.getAmount() : dto.getAmount();
        if (amount.compareTo(order.getAmount()) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "退款金额不能超过支付金额");
        }

        // 退款单号先于渠道调用生成（渠道 out_refund_no 幂等键；失败重发同号不重复退）
        PaymentRefund refund = new PaymentRefund();
        refund.setId(idGenerator.nextId());
        refund.setRefundNo(PaymentRefund.NO_PREFIX + refund.getId());
        refund.setOrderId(order.getId());
        refund.setOrderNo(order.getOrderNo());
        refund.setChannel(order.getChannel());
        refund.setChannelTradeNo(order.getChannelTradeNo());
        refund.setAmount(amount);
        refund.setReason(dto.getReason());
        refund.setOperatorMemberId(memberId);
        refund.setDeleted(0);

        RefundResult result = channelRegistry.get(order.getChannel()).refund(new RefundRequest(
                order.getOrderNo(), order.getChannelTradeNo(),
                refund.getRefundNo(), amount, dto.getReason()));
        if (!result.isAccepted()) {
            log.warn("渠道退款受理失败: orderNo={}, refundNo={}, err={}",
                    order.getOrderNo(), refund.getRefundNo(), result.getErrorMessage());
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "退款受理失败：" + result.getErrorMessage());
        }
        refund.setChannelRefundNo(result.getChannelRefundNo());
        refund.setStatus(PaymentRefund.STATUS_APPLIED);
        refundRepository.save(refund);

        // 同步终态渠道（mock/支付宝 fund_change=Y）：直接落退款成功；微信异步态保持已受理
        boolean syncFinal = isSyncFinalChannel(order.getChannel());
        if (syncFinal) {
            markRefundSuccess(refund);
        }
        // 全额退成功 → 支付单流转已退款
        if (syncFinal && amount.compareTo(order.getAmount()) == 0
                && orderRepository.casStatus(order.getId(), PaymentOrder.STATUS_SUCCESS, PaymentOrder.STATUS_REFUNDED, null, null)) {
            log.info("支付单流转已退款（全额）: orderNo={}", order.getOrderNo());
        }
        log.info("退款受理: refundNo={}, orderNo={}, amount={}, channel={}",
                refund.getRefundNo(), order.getOrderNo(), amount, order.getChannel());
        return toVO(refundRepository.findById(refund.getId()).orElse(refund));
    }

    @Override
    public PaymentRefundVO get(Long memberId, Long id) {
        PaymentRefund refund = refundRepository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.BAD_REQUEST.getCode(), "退款单不存在"));
        if (!refund.getOperatorMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权限访问该退款单");
        }
        return toVO(refund);
    }

    /** mock/支付宝退款为同步终态；微信异步处理中 */
    private boolean isSyncFinalChannel(String channel) {
        return PaymentChannelType.MOCK.equals(channel)
                || PaymentChannelType.ALIPAY.equals(channel);
    }

    private void markRefundSuccess(PaymentRefund refund) {
        refundRepository.casStatus(refund.getId(), PaymentRefund.STATUS_APPLIED,
                PaymentRefund.STATUS_SUCCESS, refund.getChannelRefundNo(), LocalDateTime.now());
    }

    private PaymentRefundVO toVO(PaymentRefund refund) {
        PaymentRefundVO vo = new PaymentRefundVO();
        vo.setId(refund.getId());
        vo.setRefundNo(refund.getRefundNo());
        vo.setOrderId(refund.getOrderId());
        vo.setOrderNo(refund.getOrderNo());
        vo.setChannel(refund.getChannel());
        vo.setAmount(refund.getAmount());
        vo.setStatus(refund.getStatus());
        vo.setStatusText(switch (refund.getStatus()) {
            case PaymentRefund.STATUS_APPLIED -> "退款处理中";
            case PaymentRefund.STATUS_SUCCESS -> "退款成功";
            case PaymentRefund.STATUS_FAILED -> "退款失败";
            default -> "未知";
        });
        vo.setReason(refund.getReason());
        vo.setChannelRefundNo(refund.getChannelRefundNo());
        vo.setRefundedAt(refund.getRefundedAt());
        vo.setCreatedAt(refund.getCreatedAt());
        return vo;
    }
}
