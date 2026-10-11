package com.xp.starsite.insurance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 保障地图：家庭成员 × 险种覆盖矩阵
 */
@Data
public class CoverageMapVO {

    /** 矩阵行：一个成员的覆盖情况 */
    @Data
    public static class MemberRow {
        private Long memberId;
        private String memberName;
        private String relation;
        /** 已配置的险种集合 */
        private List<String> categories;
    }

    private List<MemberRow> rows;

    /** 全家年缴保费合计（趸交不计入） */
    private BigDecimal totalAnnualPremium;

    /** 全家有效保单数 */
    private long activePolicyCount;
}
