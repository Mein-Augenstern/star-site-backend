package com.xp.starsite.finance.service;

import com.xp.starsite.finance.importer.BillParseResult;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 账单导入结果
 *
 * @author muyi
 * @since 2026-10
 */
@Data
@AllArgsConstructor
public class ImportResult {

    private String channel;

    /** 解析出的应入账笔数 */
    private int parsed;

    /** 实际新入账笔数 */
    private int imported;

    /** 重复跳过（uk_source_ext 幂等） */
    private int duplicates;

    /** 解析跳过（中性交易/退款/坏行） */
    private int skipped;
}
