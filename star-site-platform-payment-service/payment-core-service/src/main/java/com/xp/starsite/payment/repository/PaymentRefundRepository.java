package com.xp.starsite.payment.repository;

import com.xp.starsite.payment.entity.PaymentRefund;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 退款单仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface PaymentRefundRepository {

    /** 保存（新增；雪花 id 应用侧先赋值） */
    PaymentRefund save(PaymentRefund refund);

    /** 按退款单号取 */
    Optional<PaymentRefund> findByRefundNo(String refundNo);

    /** 按 id 取 */
    Optional<PaymentRefund> findById(Long id);

    /** 更新退款单（状态流转语义） */
    void update(PaymentRefund refund);

    /** 乐观状态流转：仅当期望状态命中才流转（渠道回调并发防重） */
    boolean casStatus(Long id, int expectStatus, int newStatus, String channelRefundNo, LocalDateTime refundedAt);

    /** 逻辑删 */
    void markDeleted(Long id);
}
