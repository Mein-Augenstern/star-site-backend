package com.xp.starsite.finance.ai.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 财务健康诊断请求（跨语言契约，字段与 Python DiagnosisRequest 对齐；数值均为字符串原样透传）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class FinanceDiagnosisRequestDTO implements Serializable {

    /** 资产负债摘要 */
    private Sheet sheet;

    /** 上月收支摘要 */
    private Month lastMonth;

    /** 现金流摘要 */
    private Cash cash;

    /** 账户清单 */
    private List<Account> accounts;

    /** 资产负债摘要 */
    @Data
    public static class Sheet implements Serializable {
        /** 总资产（元） */
        private String totalAsset;
        /** 档案资产估值（元；未知时为"未知"） */
        private String archiveAsset;
        /** archive 服务是否可用 */
        private Boolean archiveAvailable;
        /** 总负债（元） */
        private String totalLiability;
        /** 净资产（元） */
        private String netWorth;
    }

    /** 单月收支摘要 */
    @Data
    public static class Month implements Serializable {
        /** 收入（元） */
        private String income;
        /** 支出（元） */
        private String expense;
        /** 结余（元） */
        private String surplus;
    }

    /** 现金流摘要 */
    @Data
    public static class Cash implements Serializable {
        /** 流动资金（元） */
        private String liquidBalance;
        /** 可撑月数（充裕时为"充裕"） */
        private String monthsCovered;
    }

    /** 账户简要 */
    @Data
    public static class Account implements Serializable {
        /** 账户名 */
        private String name;
        /** 余额（元） */
        private String balance;
    }
}
