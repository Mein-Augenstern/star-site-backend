package com.xp.starsite.payment.repository;

import com.xp.starsite.payment.entity.PaymentOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 支付单仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface PaymentOrderRepository {

    /** 保存（新增；雪花 id 应用侧先赋值） */
    PaymentOrder save(PaymentOrder order);

    /** 按 id 取（含逻辑删过滤） */
    Optional<PaymentOrder> findById(Long id);

    /** 按支付单号取（回调路由主路径） */
    Optional<PaymentOrder> findByOrderNo(String orderNo);

    /** 按业务幂等键取最近一张支付单（source+bizType+bizId） */
    Optional<PaymentOrder> findByBiz(String source, String bizType, Long bizId);

    /** 归属账号支付单列表（可选业务过滤，按 id 倒序，家庭量级小不分页） */
    List<PaymentOrder> listByOwner(Long ownerMemberId, String source, String bizType, Long bizId);

    /** 更新支付单（状态机流转语义） */
    void update(PaymentOrder order);

    /** 乐观状态流转：仅当期望状态命中才流转（回调并发防重），false=状态已被并发迁移 */
    boolean casStatus(Long id, int expectStatus, int newStatus, String channelTradeNo, LocalDateTime paidAt);

    /** 逻辑删 */
    void markDeleted(Long id);
}
