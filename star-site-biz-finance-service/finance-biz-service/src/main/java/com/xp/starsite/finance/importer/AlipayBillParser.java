package com.xp.starsite.finance.importer;

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
 * 支付宝账单解析器。
 * 格式特征：头部含"支付宝交易记录明细查询"；列头行以"交易号"开头 16 列；
 * 金额带 ¥ 前缀；"收/支"列 = 收入/支出/不计收支。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Component
public class AlipayBillParser implements BillParser {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Map<String, String> COUNTERPART_CATEGORY = Map.of(
            "盒马鲜生", "FOOD",
            "滴滴出行", "CAR");

    @Override
    public String channel() {
        return "alipay";
    }

    @Override
    public boolean supports(String content) {
        return content.contains("支付宝交易记录明细查询") || content.contains("支付宝（中国）网络技术有限公司");
    }

    @Override
    public BillParseResult parse(String content, Long accountId, Map<String, Object> context) {
        BillParseResult result = new BillParseResult();
        List<FinanceTransaction> txs = new ArrayList<>();
        int skipped = 0;
        boolean headerSeen = false;

        for (String line : content.split("\r?\n")) {
            if (line.isBlank() || line.startsWith("-") || line.startsWith("支付宝") || line.startsWith("账号:")
                    || line.startsWith("起始日期")) {
                continue;
            }
            if (line.startsWith("交易号")) {
                headerSeen = true;
                continue;
            }
            if (!headerSeen) {
                continue;
            }
            Map<Integer, String> c = BillParserRegistry.indexColumns(line);
            String externalId = c.get(0);
            String createTime = c.get(2);
            String counterparty = c.get(7);
            String goods = c.get(8);
            String amountStr = c.get(9);
            String direction = c.get(10);
            String status = c.get(11);

            // 不计收支（退款等）与未成功交易不入账
            if (direction == null || direction.contains("不计收支")
                    || (status != null && !status.contains("成功"))) {
                skipped++;
                continue;
            }
            try {
                FinanceTransaction t = new FinanceTransaction();
                t.setDirection(direction.contains("收入") ? 1 : 2);
                t.setAmount(new BigDecimal(amountStr.replace("¥", "").trim()));
                t.setTransTime(LocalDateTime.parse(createTime.trim(), FMT));
                t.setCounterparty(BillParserRegistry.truncate(counterparty, 64));
                t.setCategory(defaultCategory(counterparty));
                t.setCategoryName(com.xp.starsite.finance.FinanceCategories.labelOf(t.getCategory()));
                t.setRemark(BillParserRegistry.truncate(goods, 255));
                t.setSource(3);
                t.setExternalId(externalId);
                t.setAccountId(accountId);
                txs.add(t);
            } catch (Exception e) {
                log.warn("支付宝账单行解析失败，跳过: {}", line);
                skipped++;
            }
        }
        result.setChannel(channel());
        result.setTransactions(txs);
        result.setSkipped(skipped);
        return result;
    }

    private String defaultCategory(String counterparty) {
        return COUNTERPART_CATEGORY.getOrDefault(counterparty, "OTHER_EXPENSE");
    }
}
