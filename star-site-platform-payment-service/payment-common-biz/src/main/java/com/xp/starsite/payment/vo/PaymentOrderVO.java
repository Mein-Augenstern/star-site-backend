package com.xp.starsite.payment.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单视图（含支付凭证，创建接口返回）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class PaymentOrderVO {

    /** 雪花 id */
    private Long id;

    /** 支付单号 */
    private String orderNo;

    /** 来源服务 */
    private String source;

    /** 业务类型 */
    private String bizType;

    /** 业务主键 */
    private Long bizId;

    /** 归属账号 id（支付人） */
    private Long ownerMemberId;

    /** 支付渠道 */
    private String channel;

    /** 商品标题（渠道账单展示） */
    private String subject;

    /** 支付金额（元） */
    private BigDecimal amount;

    /** 状态：1已创建 2成功 3已关闭 4已退款 */
    private Integer status;

    /** 状态文案 */
    private String statusText;

    /** 支付凭证（仅已创建状态返回；前端据此拉起支付/渲染收款码） */
    private String chargePayload;

    /** 渠道侧交易号 */
    private String channelTradeNo;

    /** 支付成功时间 */
    private LocalDateTime paidAt;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
