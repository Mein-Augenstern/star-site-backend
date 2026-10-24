package com.xp.starsite.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发起退款请求
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class RefundCreateDTO {

    /** 支付单 id */
    @NotNull(message = "支付单不能为空")
    private Long orderId;

    /** 退款金额（元；不传=全额退） */
    @DecimalMin(value = "0.01", message = "退款金额至少 0.01 元")
    @Digits(integer = 10, fraction = 2, message = "金额最多两位小数")
    private BigDecimal amount;

    /** 退款原因 */
    private String reason;
}
