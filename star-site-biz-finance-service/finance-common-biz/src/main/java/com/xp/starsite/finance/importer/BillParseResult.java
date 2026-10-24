package com.xp.starsite.finance.importer;

import com.xp.starsite.finance.entity.FinanceTransaction;
import lombok.Data;

import java.util.List;

/**
 * 账单解析结果
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class BillParseResult {

    /** 渠道（wechat/alipay） */
    private String channel;

    /** 解析出的流水（source=2/3，external_id=账单方交易号；金额恒正，方向由 direction 表达） */
    private List<FinanceTransaction> transactions;

    /** 跳过行数（中性交易/退款/重复表头等不入账行） */
    private int skipped;
}
