package com.xp.starsite.payment.dal.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单数据对象（payment_order 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/PaymentOrder。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class PaymentOrderDO {

    /** 主键（雪花 id，应用侧赋值） */
    private Long id;

    /** 支付单号（uk） */
    private String orderNo;

    /** 来源服务（幂等键第一段） */
    private String source;

    /** 业务类型（幂等键第二段） */
    private String bizType;

    /** 业务主键（幂等键第三段） */
    private Long bizId;

    /** 家庭 id（可空） */
    private Long familyId;

    /** 归属账号 id */
    private Long ownerMemberId;

    /** 支付渠道 */
    private String channel;

    /** 商品标题 */
    private String subject;

    /** 支付金额（元） */
    private BigDecimal amount;

    /** 状态：1已创建 2成功 3已关闭 4已退款 */
    private Integer status;

    /** 渠道侧交易号 */
    private String channelTradeNo;

    /** 支付成功时间 */
    private LocalDateTime paidAt;

    /** 支付凭证 */
    private String chargePayload;

    /** 备注 */
    private String remark;

    /** 逻辑删标志（0 正常 1 已删） */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
