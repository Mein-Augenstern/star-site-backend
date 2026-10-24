package com.xp.starsite.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单快照（跨服务传输用）：业务服务（保险代收/会员订阅）经 PaymentInternalApi
 * 下单与查单时的契约载体。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class PaymentOrderDTO implements Serializable {

    /** 雪花 id */
    private Long id;

    /** 支付单号 */
    private String orderNo;

    /** 来源服务（幂等键第一段） */
    private String source;

    /** 业务类型（幂等键第二段） */
    private String bizType;

    /** 业务主键（幂等键第三段） */
    private Long bizId;

    /** 归属账号 id（支付人） */
    private Long ownerMemberId;

    /** 支付渠道：mock/alipay/wechat */
    private String channel;

    /** 商品标题（渠道账单展示） */
    private String subject;

    /** 支付金额（元） */
    private BigDecimal amount;

    /** 状态：1已创建 2成功 3已关闭 4已退款 */
    private Integer status;

    /** 支付凭证（仅已创建状态非空） */
    private String chargePayload;

    /** 渠道侧交易号 */
    private String channelTradeNo;

    /** 支付成功时间 */
    private LocalDateTime paidAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
