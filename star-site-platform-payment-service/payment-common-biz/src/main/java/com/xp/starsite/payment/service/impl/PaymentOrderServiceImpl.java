package com.xp.starsite.payment.service.impl;

import com.xp.starsite.api.client.PaymentOrderCreateInternalDTO;
import com.xp.starsite.api.dto.PaymentOrderDTO;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.common.mq.EventPublisher;
import com.xp.starsite.common.mq.event.EventTypes;
import com.xp.starsite.common.mq.event.Topics;
import com.xp.starsite.common.util.IdGenerator;
import com.xp.starsite.payment.channel.CallbackResult;
import com.xp.starsite.payment.channel.ChargeRequest;
import com.xp.starsite.payment.channel.ChargeResult;
import com.xp.starsite.payment.channel.PaymentChannel;
import com.xp.starsite.payment.constant.PaymentChannelType;
import com.xp.starsite.payment.dto.PaymentOrderCreateDTO;
import com.xp.starsite.payment.entity.PaymentOrder;
import com.xp.starsite.payment.integration.channel.PaymentChannelRegistry;
import com.xp.starsite.payment.repository.PaymentOrderRepository;
import com.xp.starsite.payment.service.PaymentOrderService;
import com.xp.starsite.payment.vo.PaymentOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 支付单服务实现：下单幂等（同业务键复用在途单）→ 渠道路由预下单 → 回调 CAS 状态机
 * → 事务消息发布支付成功事件（MQ 停用期发布失败不阻断入账，业务方以内部查单兜底）。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrderServiceImpl implements PaymentOrderService {

    /** 渠道应答体：alipay/mock 收到 "success" 停止重试；微信端见 WechatAck（JSON） */
    private static final String CHANNEL_ACK_SUCCESS = "success";
    private static final String WECHAT_ACK_SUCCESS = "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";

    private final PaymentOrderRepository orderRepository;
    private final PaymentChannelRegistry channelRegistry;
    private final IdGenerator idGenerator;
    private final EventPublisher eventPublisher;

    /** 渠道服务器回调基地址（star.payment.notify-url，如公网隧道/域名；拼接 /api/payment/notify/{channel}） */
    @Value("${star.payment.notify-url:}")
    private String notifyUrlBase;

    @Override
    public PaymentOrderVO create(Long memberId, PaymentOrderCreateDTO dto) {
        PaymentChannel channel = channelRegistry.get(dto.getChannel());
        // 下单幂等：同业务键已有单 → 在途复用 / 成功拒绝 / 关闭退款重开
        PaymentOrder existing = orderRepository.findByBiz(dto.getSource(), dto.getBizType(), dto.getBizId()).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == PaymentOrder.STATUS_SUCCESS) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(), "该业务单已支付成功，请勿重复下单");
            }
            if (existing.getStatus() == PaymentOrder.STATUS_REFUNDED) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(), "该业务单已退款，请走新的业务流程");
            }
            // CREATED：复用在途单重新取凭证；CLOSED：重开（换渠道也在此路径）
            return recharge(existing, channel, memberId, dto);
        }

        PaymentOrder order = new PaymentOrder();
        order.setId(idGenerator.nextId());
        order.setOrderNo(PaymentOrder.NO_PREFIX + order.getId());
        order.setSource(dto.getSource());
        order.setBizType(dto.getBizType());
        order.setBizId(dto.getBizId());
        order.setFamilyId(dto.getFamilyId());
        order.setOwnerMemberId(memberId);
        order.setChannel(dto.getChannel());
        order.setSubject(dto.getSubject());
        order.setAmount(dto.getAmount());
        order.setStatus(PaymentOrder.STATUS_CREATED);
        order.setRemark(dto.getRemark());
        order.setDeleted(0);
        order.setChargePayload(chargeOf(channel, order).getPayload());
        orderRepository.save(order);
        log.info("支付单创建: orderNo={}, biz={}.{}, bizId={}, channel={}, amount={}",
                order.getOrderNo(), order.getSource(), order.getBizType(), order.getBizId(), order.getChannel(), order.getAmount());
        return toVO(order);
    }

    @Override
    public PaymentOrderVO get(Long memberId, Long id) {
        return toVO(requireOwned(memberId, id));
    }

    @Override
    public List<PaymentOrderVO> list(Long memberId, String source, String bizType, Long bizId) {
        return orderRepository.listByOwner(memberId, source, bizType, bizId).stream().map(this::toVO).toList();
    }

    @Override
    public PaymentOrderVO getByBiz(Long memberId, String source, String bizType, Long bizId) {
        PaymentOrder order = orderRepository.findByBiz(source, bizType, bizId).orElse(null);
        return order == null ? null : toVO(order);
    }

    @Override
    public void close(Long memberId, Long id) {
        PaymentOrder order = requireOwned(memberId, id);
        if (order.getStatus() != PaymentOrder.STATUS_CREATED) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "仅待支付订单可关闭");
        }
        channelRegistry.get(order.getChannel()).close(order.getOrderNo());
        if (!orderRepository.casStatus(id, PaymentOrder.STATUS_CREATED, PaymentOrder.STATUS_CLOSED, null, null)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "订单状态已变化（可能已支付），请刷新查看");
        }
        log.info("支付单关闭: orderNo={}", order.getOrderNo());
    }

    @Override
    public PaymentOrderDTO createInternal(Long memberId, PaymentOrderCreateInternalDTO internal) {
        PaymentOrderCreateDTO dto = new PaymentOrderCreateDTO();
        dto.setSource(internal.getSource());
        dto.setBizType(internal.getBizType());
        dto.setBizId(internal.getBizId());
        dto.setFamilyId(internal.getFamilyId());
        dto.setChannel(internal.getChannel());
        dto.setSubject(internal.getSubject());
        dto.setAmount(internal.getAmount());
        dto.setRemark(internal.getRemark());
        return toDto(create(memberId, dto));
    }

    @Override
    public PaymentOrderDTO getByBizInternal(Long memberId, String source, String bizType, Long bizId) {
        PaymentOrder order = orderRepository.findByBiz(source, bizType, bizId).orElse(null);
        return order == null ? null : toDto(toVO(order));
    }

    @Override
    public String handleNotify(String channelType, Map<String, String> headers, String rawBody) {
        PaymentChannel channel = channelRegistry.get(channelType);
        CallbackResult callback = channel.verifyAndParse(headers, rawBody);
        if (!callback.isSuccess()) {
            // 非支付成功类通知（如关闭通知）：应答成功让渠道停止重发
            log.info("渠道非成功通知，忽略: channel={}, orderNo={}", channelType, callback.getOrderNo());
            return ackOf(channelType);
        }
        PaymentOrder order = orderRepository.findByOrderNo(callback.getOrderNo()).orElse(null);
        if (order == null) {
            log.error("回调支付单不存在: channel={}, orderNo={}", channelType, callback.getOrderNo());
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "支付单不存在");
        }
        // 金额核对：渠道入账金额必须与应付一致，不符拒绝应答（渠道重试，人工介入排查）
        if (order.getAmount().compareTo(callback.getPaidAmount()) != 0) {
            log.error("回调金额不符，拒绝入账: orderNo={}, expect={}, paid={}",
                    order.getOrderNo(), order.getAmount(), callback.getPaidAmount());
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "回调金额与应付不符");
        }
        // CAS 状态机：CREATED → SUCCESS 仅一次；重放（已成功）幂等应答
        boolean moved = orderRepository.casStatus(order.getId(),
                PaymentOrder.STATUS_CREATED, PaymentOrder.STATUS_SUCCESS,
                callback.getChannelTradeNo(), LocalDateTime.now());
        if (!moved) {
            log.info("回调重放或状态已迁移，幂等应答: orderNo={}, status={}", order.getOrderNo(),
                    orderRepository.findById(order.getId()).map(PaymentOrder::getStatus).orElse(-1));
            return ackOf(channelType);
        }
        // 事件增强链路：MQ 停用期发布失败仅告警，入账不受影响（业务方以内部查单兜底）
        try {
            com.xp.starsite.api.event.payment.PaymentSucceededEvent event =
                    new com.xp.starsite.api.event.payment.PaymentSucceededEvent();
            event.setOrderNo(order.getOrderNo());
            event.setSource(order.getSource());
            event.setBizType(order.getBizType());
            event.setBizId(order.getBizId());
            event.setOwnerMemberId(order.getOwnerMemberId());
            event.setChannel(order.getChannel());
            event.setChannelTradeNo(callback.getChannelTradeNo());
            event.setAmount(order.getAmount());
            event.setPaidAt(LocalDateTime.now());
            eventPublisher.publish(Topics.TOPIC_PAYMENT,
                    EventTypes.PAYMENT_SUCCEEDED,
                    order.getBizType(), order.getBizId(), event);
        } catch (Exception e) {
            log.warn("支付成功事件发布失败（不阻断入账）: orderNo={}, err={}", order.getOrderNo(), e.getMessage());
        }
        log.info("支付成功入账: orderNo={}, channel={}, tradeNo={}, amount={}",
                order.getOrderNo(), channelType, callback.getChannelTradeNo(), callback.getPaidAmount());
        return ackOf(channelType);
    }

    /** 在途单重新取凭证（换渠道也走此路径） */
    private PaymentOrderVO recharge(PaymentOrder existing, PaymentChannel channel, Long memberId, PaymentOrderCreateDTO dto) {
        requireOwned(memberId, existing.getId());
        existing.setChannel(dto.getChannel());
        existing.setSubject(dto.getSubject());
        existing.setAmount(dto.getAmount());
        existing.setRemark(dto.getRemark());
        existing.setStatus(PaymentOrder.STATUS_CREATED);
        existing.setChargePayload(chargeOf(channel, existing).getPayload());
        orderRepository.update(existing);
        return toVO(existing);
    }

    /** 渠道预下单（凭证落单，供查询/重取） */
    private ChargeResult chargeOf(PaymentChannel channel, PaymentOrder order) {
        ChargeResult result = channel.charge(new ChargeRequest(
                order.getOrderNo(), order.getSubject(), order.getAmount(), notifyUrlOf(channel.channelType())));
        order.setChargePayload(result.getPayload());
        return result;
    }

    private String notifyUrlOf(String channelType) {
        // 渠道服务器回调入口；base 由 star.payment.notify-url 配置（内网穿透/域名就绪后切换）
        return notifyUrlBase + "/api/payment/notify/" + channelType;
    }

    private String ackOf(String channelType) {
        return PaymentChannelType.WECHAT.equals(channelType) ? WECHAT_ACK_SUCCESS : CHANNEL_ACK_SUCCESS;
    }

    /** 归属防线：支付单必须存在且归属当前账号（fail-closed） */
    private PaymentOrder requireOwned(Long memberId, Long id) {
        PaymentOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.BAD_REQUEST.getCode(), "支付单不存在"));
        // 支付单行的 owner_member_id 在下单时固化为调用方账号，等值校验即可收口归属
        if (!order.getOwnerMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权限访问该支付单");
        }
        return order;
    }

    /** VO → facade 快照（内部契约出参；indicators 类敏感字段不透出） */
    private PaymentOrderDTO toDto(PaymentOrderVO vo) {
        PaymentOrderDTO dto = new PaymentOrderDTO();
        dto.setId(vo.getId());
        dto.setOrderNo(vo.getOrderNo());
        dto.setSource(vo.getSource());
        dto.setBizType(vo.getBizType());
        dto.setBizId(vo.getBizId());
        dto.setOwnerMemberId(vo.getOwnerMemberId());
        dto.setChannel(vo.getChannel());
        dto.setSubject(vo.getSubject());
        dto.setAmount(vo.getAmount());
        dto.setStatus(vo.getStatus());
        dto.setChargePayload(vo.getChargePayload());
        dto.setChannelTradeNo(vo.getChannelTradeNo());
        dto.setPaidAt(vo.getPaidAt());
        dto.setCreatedAt(vo.getCreatedAt());
        return dto;
    }

    private PaymentOrderVO toVO(PaymentOrder order) {
        PaymentOrderVO vo = new PaymentOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setSource(order.getSource());
        vo.setBizType(order.getBizType());
        vo.setBizId(order.getBizId());
        vo.setOwnerMemberId(order.getOwnerMemberId());
        vo.setChannel(order.getChannel());
        vo.setSubject(order.getSubject());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(switch (order.getStatus()) {
            case PaymentOrder.STATUS_CREATED -> "待支付";
            case PaymentOrder.STATUS_SUCCESS -> "已支付";
            case PaymentOrder.STATUS_CLOSED -> "已关闭";
            case PaymentOrder.STATUS_REFUNDED -> "已退款";
            default -> "未知";
        });
        // 凭证仅待支付单返回（已支付/关闭后凭证无意义且不应再被拉起）
        vo.setChargePayload(order.getStatus() == PaymentOrder.STATUS_CREATED ? order.getChargePayload() : null);
        vo.setChannelTradeNo(order.getChannelTradeNo());
        vo.setPaidAt(order.getPaidAt());
        vo.setRemark(order.getRemark());
        vo.setCreatedAt(order.getCreatedAt());
        return vo;
    }
}
