package com.xp.starsite.payment.channel;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 渠道退款受理结果
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@AllArgsConstructor
public class RefundResult {

    /** 渠道是否受理成功（失败时 error 信息见 errorMessage） */
    private boolean accepted;

    /** 渠道侧退款单号（受理成功时回填，可空=同步未返回走查询） */
    private String channelRefundNo;

    /** 受理失败原因 */
    private String errorMessage;
}
