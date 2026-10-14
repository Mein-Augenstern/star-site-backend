package com.xp.starsite.payment.repository.impl;

import com.xp.starsite.payment.converter.PaymentRefundConverter;
import com.xp.starsite.payment.dal.dataobject.PaymentRefundDO;
import com.xp.starsite.payment.entity.PaymentRefund;
import com.xp.starsite.payment.mapper.PaymentRefundMapper;
import com.xp.starsite.payment.repository.PaymentRefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 退款单仓储实现：调用 dal 的 {@link PaymentRefundMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class PaymentRefundRepositoryImpl implements PaymentRefundRepository {

    private final PaymentRefundMapper paymentRefundMapper;

    @Override
    public PaymentRefund save(PaymentRefund refund) {
        paymentRefundMapper.insert(PaymentRefundConverter.toDO(refund));
        return refund;
    }

    @Override
    public Optional<PaymentRefund> findByRefundNo(String refundNo) {
        return Optional.ofNullable(paymentRefundMapper.selectByRefundNo(refundNo)).map(PaymentRefundConverter::toEntity);
    }

    @Override
    public Optional<PaymentRefund> findById(Long id) {
        return Optional.ofNullable(paymentRefundMapper.selectById(id)).map(PaymentRefundConverter::toEntity);
    }

    @Override
    public void update(PaymentRefund refund) {
        paymentRefundMapper.update(PaymentRefundConverter.toDO(refund));
    }

    @Override
    public boolean casStatus(Long id, int expectStatus, int newStatus, String channelRefundNo, LocalDateTime refundedAt) {
        return paymentRefundMapper.casStatus(id, expectStatus, newStatus, channelRefundNo, refundedAt) > 0;
    }

    @Override
    public void markDeleted(Long id) {
        paymentRefundMapper.markDeleted(id);
    }
}
