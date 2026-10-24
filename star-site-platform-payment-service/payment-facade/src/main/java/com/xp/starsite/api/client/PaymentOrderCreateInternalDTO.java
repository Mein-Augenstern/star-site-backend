package com.xp.starsite.api.client;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 业务服务代下单请求（PaymentInternalApi 专用；与 C 端 PaymentOrderCreateDTO 字段对齐，
 * 独立成类避免 facade 依赖 common-biz 的 DTO）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class PaymentOrderCreateInternalDTO {

    /** 来源服务（幂等键第一段，必须为调用方自身服务名） */
    @NotBlank(message = "来源服务不能为空")
    private String source;

    /** 业务类型（幂等键第二段） */
    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    /** 业务主键（幂等键第三段） */
    @NotNull(message = "业务主键不能为空")
    private Long bizId;

    /** 家庭 id（业务透传冗余，可空） */
    private Long familyId;

    /** 支付渠道：mock/alipay/wechat */
    @NotBlank(message = "支付渠道不能为空")
    private String channel;

    /** 商品标题 */
    @NotBlank(message = "商品标题不能为空")
    private String subject;

    /** 支付金额（元） */
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额至少 0.01 元")
    @Digits(integer = 10, fraction = 2, message = "金额最多两位小数")
    private BigDecimal amount;

    /** 备注（可空） */
    private String remark;
}
