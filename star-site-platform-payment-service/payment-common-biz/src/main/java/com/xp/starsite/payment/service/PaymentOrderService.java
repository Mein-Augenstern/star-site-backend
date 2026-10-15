package com.xp.starsite.payment.service;

import com.xp.starsite.payment.dto.PaymentOrderCreateDTO;
import com.xp.starsite.payment.vo.PaymentOrderVO;

import java.util.List;
import java.util.Map;

/**
 * 支付单服务：统一收银台（下单 → 渠道回调状态机 → 关单/退款入口）。
 * <p>
 * 归属防线约定：方法首参 memberId 为当前登录账号（Controller 从 JWT attribute 取），
 * 查询/关单按归属账号收口；回调入口（handleNotify）不经 JWT、以渠道验签为唯一防线。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface PaymentOrderService {

    /**
     * 创建支付单（同业务键已有在途单则复用返回，已支付/已退款则拒绝，已关闭可重新发起）
     *
     * @param memberId 当前登录账号（支付人）
     * @param dto      下单内容
     * @return 支付单视图（含支付凭证）
     * @throws com.xp.starsite.common.exception.BizException 400 业务已支付/渠道未装配；参数校验失败
     */
    PaymentOrderVO create(Long memberId, PaymentOrderCreateDTO dto);

    /**
     * 支付单详情（仅归属人可见）
     *
     * @throws com.xp.starsite.common.exception.BizException 400 不存在；403 非本人支付单
     */
    PaymentOrderVO get(Long memberId, Long id);

    /**
     * 支付单列表（归属账号范围，可按业务键过滤）
     */
    List<PaymentOrderVO> list(Long memberId, String source, String bizType, Long bizId);

    /**
     * 按业务幂等键查询（内部契约/业务方同步用，按归属账号收口）
     */
    PaymentOrderVO getByBiz(Long memberId, String source, String bizType, Long bizId);

    /**
     * 关闭在途支付单（仅归属人、仅已创建状态可关）
     *
     * @throws com.xp.starsite.common.exception.BizException 400 不存在/状态不允许；403 非本人
     */
    void close(Long memberId, Long id);

    /**
     * 业务代下单（内部契约）：同业务键在途单复用返回，已支付/已退款拒绝。
     * 转换收敛在本服务（InternalDTO 组装 + VO→facade DTO 映射），控制器零映射。
     *
     * @param memberId 支付人账号
     * @return 支付单快照（含支付凭证）
     */
    com.xp.starsite.api.dto.PaymentOrderDTO createInternal(Long memberId,
                                                           com.xp.starsite.api.client.PaymentOrderCreateInternalDTO dto);

    /**
     * 按业务幂等键查单（内部契约，供业务方驱动状态流转）；尚无支付单时返回 null
     */
    com.xp.starsite.api.dto.PaymentOrderDTO getByBizInternal(Long memberId, String source,
                                                             String bizType, Long bizId);

    /**
     * 渠道异步回调处理（验签 → 金额核对 → CAS 状态流转 → 发布支付成功事件）。
     * 幂等：重复回调（CAS 不命中）按首次处理结果直接应答成功。
     *
     * @param channelType 渠道路由键（回调 URL 路径段）
     * @param headers     回调请求头（微信 v3 验签四件套）
     * @param rawBody     回调原始报文
     * @return 渠道应答体（alipay/mock="success"；wechat={"code":"SUCCESS"}），调用方原样写回
     * @throws com.xp.starsite.common.exception.BizException 验签失败（401）/金额不符（400）——渠道会重试
     */
    String handleNotify(String channelType, Map<String, String> headers, String rawBody);
}
