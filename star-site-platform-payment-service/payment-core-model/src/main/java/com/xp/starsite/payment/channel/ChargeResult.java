package com.xp.starsite.payment.channel;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 预下单结果：前端拉起支付所需的凭证
 *
 * @author muyi
 * @since 2026-10
 */
@Data
@AllArgsConstructor
public class ChargeResult {

    /** 支付凭证（mock=沙箱 token；alipay=二维码内容 qrcode；wechat=code_url） */
    private String payload;
}
