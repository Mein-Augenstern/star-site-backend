package com.xp.starsite.payment.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单（payment_order）：统一收银台的主体表。
 * 一张支付单 = 一笔业务交易的一次收款 attempt：业务服务（保险代收/会员订阅/…）
 * 经 facade 或 C 端创建，渠道路由 → 预下单拿支付凭证 → 渠道异步回调驱动状态机。
 * 幂等约束：uk_order_no（应用侧雪花单号）、uk_biz（source+biz_type+biz_id 在途唯一），
 * 渠道回调以 uk_channel_trade（channel+channel_trade_no）防重放。
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class PaymentOrder {

    /** 支付单号前缀（渠道 out_trade_no 用） */
    public static final String NO_PREFIX = "PP";

    /** 已创建（凭证已生成，等待用户支付） */
    public static final int STATUS_CREATED = 1;

    /** 支付成功（渠道回调确认，终态前状态：可退款） */
    public static final int STATUS_SUCCESS = 2;

    /** 已关闭（超时未付/主动关闭，终态） */
    public static final int STATUS_CLOSED = 3;

    /** 已全额退款（终态） */
    public static final int STATUS_REFUNDED = 4;

    /** 雪花 id（应用侧赋值） */
    private Long id;

    /** 支付单号（对外可见，PP 前缀 + 雪花；渠道侧 out_trade_no） */
    private String orderNo;

    /** 来源服务（业务归属，幂等键第一段，如 star-site-biz-insurance） */
    private String source;

    /** 业务类型（幂等键第二段，如 insurance_policy / member_subscription） */
    private String bizType;

    /** 业务主键（幂等键第三段，业务方的单据 id） */
    private Long bizId;

    /** 家庭 id（业务透传冗余，可空） */
    private Long familyId;

    /** 归属账号 id（支付人） */
    private Long ownerMemberId;

    /** 支付渠道：mock/alipay/wechat（见 PaymentChannelType） */
    private String channel;

    /** 商品标题（渠道账单展示） */
    private String subject;

    /** 支付金额（元，DECIMAL(12,2)） */
    private BigDecimal amount;

    /** 状态：1已创建 2成功 3已关闭 4已退款 */
    private Integer status;

    /** 渠道侧交易号（回调回填；mock 渠道为 MOCK+雪花） */
    private String channelTradeNo;

    /** 支付成功时间 */
    private LocalDateTime paidAt;

    /** 支付凭证（预下单产物：二维码内容/跳转链接/沙箱 token，前端据此拉起支付） */
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
