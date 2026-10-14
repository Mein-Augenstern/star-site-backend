package com.xp.starsite.payment.mapper;

import com.xp.starsite.payment.dal.dataobject.PaymentRefundDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 退款单数据访问（DAL）：payment_refund，逻辑删。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface PaymentRefundMapper {

    /** 新建退款单（雪花 id 应用侧赋值） */
    int insert(PaymentRefundDO refund);

    /** 按退款单号取（不存在返回 null） */
    PaymentRefundDO selectByRefundNo(@Param("refundNo") String refundNo);

    /** 按 id 取 */
    PaymentRefundDO selectById(@Param("id") Long id);

    /** 更新退款单（状态流转） */
    int update(PaymentRefundDO refund);

    /** 乐观状态流转：仅当当前状态为 expectStatus 时更新（渠道回调并发防重） */
    int casStatus(@Param("id") Long id,
                  @Param("expectStatus") int expectStatus,
                  @Param("newStatus") int newStatus,
                  @Param("channelRefundNo") String channelRefundNo,
                  @Param("refundedAt") java.time.LocalDateTime refundedAt);

    /** 逻辑删 */
    int markDeleted(@Param("id") Long id);
}
