package com.xp.starsite.payment.controller.actions;

/**
 * 支付域动作名常量（模板留痕 action 字段，规范：服务.资源.动作）
 * <p>
 * 动作名会进模板留痕与 digest，只能新增、不能改名：字符串值与历史值逐字节一致。
 * 常量名取「资源.动作」段，省略服务前缀 payment.。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class PaymentActions {

    // —— 支付单 ——

    /** 创建支付单 */
    public static final String ORDER_CREATE = "payment.order.create";

    /** 支付单详情 */
    public static final String ORDER_GET = "payment.order.get";

    /** 支付单列表 */
    public static final String ORDER_LIST = "payment.order.list";

    /** 按业务键查单 */
    public static final String ORDER_GET_BY_BIZ = "payment.order.getByBiz";

    /** 关闭支付单 */
    public static final String ORDER_CLOSE = "payment.order.close";

    // —— 渠道回调 ——

    /** 渠道异步回调（验签+状态机） */
    public static final String NOTIFY_HANDLE = "payment.notify.handle";

    // —— 退款 ——

    /** 发起退款 */
    public static final String REFUND_APPLY = "payment.refund.apply";

    /** 退款单详情 */
    public static final String REFUND_GET = "payment.refund.get";

    // —— 服务间内部接口 ——

    /** 业务代下单 */
    public static final String INTERNAL_ORDER_CREATE = "payment.internal.orderCreate";

    /** 按业务键查单（内部） */
    public static final String INTERNAL_ORDER_GET_BY_BIZ = "payment.internal.orderGetByBiz";

    private PaymentActions() {
    }
}
