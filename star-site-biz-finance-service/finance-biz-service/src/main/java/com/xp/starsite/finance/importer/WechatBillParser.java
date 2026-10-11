package com.xp.starsite.finance.importer;

import com.xp.starsite.finance.FinanceCategories;
import com.xp.starsite.finance.entity.FinanceTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 微信支付账单解析器。
 * 格式特征：头部含"微信支付账单明细"；列头行以"交易时间"开头 11 列；
 * 金额带 ¥ 前缀；"收/支"列 = 收入/支出/中性交易（"/" 与"退款"不计支出）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Component
public class WechatBillParser implements BillParser {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 微信交易类型 → 分类枚举的粗映射（未命中归其他支出） */
    private static final Map<String, String> TYPE_CATEGORY = Map.of(
            "商户消费", "SHOPPING",
            "微信红包", "RED_PACKET",
            "转账", "OTHER_EXPENSE",
            "二维码收款", "OTHER_INCOME");

    @Override
    public String channel() {
        return "wechat";
    }

    @Override
    public boolean supports(String content) {
        return content.contains("微信支付账单明细");
    }

    @Override
    public BillParseResult parse(String content, Long accountId, Map<String, Object> context) {
        BillParseResult result = new BillParseResult();
        List<FinanceTransaction> txs = new ArrayList<>();
        int skipped = 0;
        boolean headerSeen = false;

        for (String line : content.split("\r?\n")) {
            if (line.isBlank()) {
                continue;
            }
            if (line.startsWith("交易时间")) {
                headerSeen = true;
                continue;
            }
            if (!headerSeen || line.startsWith("-")) {
                continue;
            }
            Map<Integer, String> c = BillParserRegistry.indexColumns(line);
            String timeStr = c.get(0);
            String type = c.get(1);
            String counterparty = c.get(2);
            String direction = c.get(4);
            String amountStr = c.get(5);
            String status = c.get(7);
            String externalId = c.get(8);

            // 中性交易（"/"）、退款、未成功的支付不计入
            if (direction == null || direction.equals("/") || direction.contains("退款")
                    || (status != null && !status.contains("成功") && !status.contains("存入") && !status.contains("收钱"))) {
                skipped++;
                continue;
            }
            try {
                FinanceTransaction t = new FinanceTransaction();
                t.setDirection(direction.contains("收入") ? 1 : 2);
                t.setAmount(new BigDecimal(amountStr.replace("¥", "").trim()));
                t.setTransTime(LocalDateTime.parse(timeStr.trim(), FMT));
                t.setCounterparty(BillParserRegistry.truncate(counterparty, 64));
                t.setCategory(defaultCategory(direction, type));
                t.setCategoryName(com.xp.starsite.finance.FinanceCategories.labelOf(t.getCategory()));
                t.setRemark(BillParserRegistry.truncate(type + " " + c.get(3), 255));
                t.setSource(2);
                t.setExternalId(externalId);
                t.setAccountId(accountId);
                txs.add(t);
            } catch (Exception e) {
                log.warn("微信账单行解析失败，跳过: {}", line);
                skipped++;
            }
        }
        result.setChannel(channel());
        result.setTransactions(txs);
        result.setSkipped(skipped);
        return result;
    }

    private String defaultCategory(String direction, String type) {
        if (direction.contains("收入")) {
            return "OTHER_INCOME";
        }
        return TYPE_CATEGORY.getOrDefault(type, "OTHER_EXPENSE");
    }

}
