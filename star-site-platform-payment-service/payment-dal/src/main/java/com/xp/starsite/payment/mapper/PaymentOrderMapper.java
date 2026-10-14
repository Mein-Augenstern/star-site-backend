package com.xp.starsite.payment.mapper;

import com.xp.starsite.payment.dal.dataobject.PaymentOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 支付单数据访问（DAL）：payment_order，逻辑删。
 * SQL 全部收敛在 mapper XML，业务层只见业务语义方法。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface PaymentOrderMapper {

    /** 新建支付单（雪花 id 应用侧赋值：事件载荷需要在 insert 前拿到 id） */
    int insert(PaymentOrderDO order);

    /** 按 id 取支付单（含逻辑删过滤，不存在返回 null） */
    PaymentOrderDO selectById(@Param("id") Long id);

    /** 按支付单号取支付单（回调路由主路径） */
    PaymentOrderDO selectByOrderNo(@Param("orderNo") String orderNo);

    /** 按业务幂等键取在途/最近支付单（source+bizType+bizId） */
    PaymentOrderDO selectByBiz(@Param("source") String source,
                               @Param("bizType") String bizType,
                               @Param("bizId") Long bizId);

    /** 归属账号的支付单列表（按 id 倒序，家庭量级小不分页） */
    List<PaymentOrderDO> selectByOwner(@Param("ownerMemberId") Long ownerMemberId,
                                       @Param("source") String source,
                                       @Param("bizType") String bizType,
                                       @Param("bizId") Long bizId);

    /** 更新支付单（按 id，状态机流转语义） */
    int update(PaymentOrderDO order);

    /** 乐观状态流转：仅当当前状态为 expectStatus 时更新为 newStatus（回调并发防重），返回影响行数 */
    int casStatus(@Param("id") Long id,
                  @Param("expectStatus") int expectStatus,
                  @Param("newStatus") int newStatus,
                  @Param("channelTradeNo") String channelTradeNo,
                  @Param("paidAt") java.time.LocalDateTime paidAt);

    /** 逻辑删：deleted 置 1 */
    int markDeleted(@Param("id") Long id);
}
