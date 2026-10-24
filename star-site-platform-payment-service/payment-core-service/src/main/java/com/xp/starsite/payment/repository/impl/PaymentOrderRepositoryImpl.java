package com.xp.starsite.payment.repository.impl;

import com.xp.starsite.payment.converter.PaymentOrderConverter;
import com.xp.starsite.payment.dal.dataobject.PaymentOrderDO;
import com.xp.starsite.payment.entity.PaymentOrder;
import com.xp.starsite.payment.mapper.PaymentOrderMapper;
import com.xp.starsite.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 支付单仓储实现：调用 dal 的 {@link PaymentOrderMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class PaymentOrderRepositoryImpl implements PaymentOrderRepository {

    private final PaymentOrderMapper paymentOrderMapper;

    @Override
    public PaymentOrder save(PaymentOrder order) {
        paymentOrderMapper.insert(PaymentOrderConverter.toDO(order));
        return order;
    }

    @Override
    public Optional<PaymentOrder> findById(Long id) {
        return Optional.ofNullable(paymentOrderMapper.selectById(id)).map(PaymentOrderConverter::toEntity);
    }

    @Override
    public Optional<PaymentOrder> findByOrderNo(String orderNo) {
        return Optional.ofNullable(paymentOrderMapper.selectByOrderNo(orderNo)).map(PaymentOrderConverter::toEntity);
    }

    @Override
    public Optional<PaymentOrder> findByBiz(String source, String bizType, Long bizId) {
        return Optional.ofNullable(paymentOrderMapper.selectByBiz(source, bizType, bizId)).map(PaymentOrderConverter::toEntity);
    }

    @Override
    public List<PaymentOrder> listByOwner(Long ownerMemberId, String source, String bizType, Long bizId) {
        return paymentOrderMapper.selectByOwner(ownerMemberId, source, bizType, bizId)
                .stream().map(PaymentOrderConverter::toEntity).toList();
    }

    @Override
    public void update(PaymentOrder order) {
        paymentOrderMapper.update(PaymentOrderConverter.toDO(order));
    }

    @Override
    public boolean casStatus(Long id, int expectStatus, int newStatus, String channelTradeNo, LocalDateTime paidAt) {
        return paymentOrderMapper.casStatus(id, expectStatus, newStatus, channelTradeNo, paidAt) > 0;
    }

    @Override
    public void markDeleted(Long id) {
        paymentOrderMapper.markDeleted(id);
    }
}
