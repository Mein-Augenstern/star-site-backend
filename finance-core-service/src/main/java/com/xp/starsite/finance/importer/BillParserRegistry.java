package com.xp.starsite.finance.importer;

import com.xp.starsite.finance.entity.FinanceTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 解析器注册表：收集全部 BillParser 实现，按格式探测选择。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class BillParserRegistry {

    private final List<BillParser> parsers;

    /** 按账单文本自动探测解析器；无匹配抛业务异常 */
    public BillParser detect(String content) {
        return parsers.stream()
                .filter(p -> p.supports(content))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "无法识别账单格式，目前支持微信支付/支付宝导出的 CSV 账单"));
    }

    public BillParser byChannel(String channel) {
        return parsers.stream()
                .filter(p -> p.channel().equalsIgnoreCase(channel))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的账单渠道: " + channel));
    }

    /** 防御：注册表为空时给出明确错误（正常不会发生） */
    private static <T> T requireNonNull(T t) {
        if (t == null) {
            throw new IllegalStateException("no parser");
        }
        return t;
    }

    /** 字段截断（counterparty/remark 落库上限保护） */
    static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    /** 工具：行内逗号切分（账单字段不含引号内逗号的简化假设；微信/支付宝导出实测满足） */
    static Map<Integer, String> indexColumns(String line) {
        String[] cols = line.split(",", -1);
        return java.util.stream.IntStream.range(0, cols.length)
                .boxed()
                .collect(Collectors.toMap(Function.identity(), i -> cols[i].trim(), (a, b) -> a));
    }
}
