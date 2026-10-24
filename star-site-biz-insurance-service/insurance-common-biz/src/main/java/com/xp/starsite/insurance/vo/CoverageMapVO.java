package com.xp.starsite.insurance.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 保障地图：家庭成员 × 险种覆盖矩阵
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class CoverageMapVO {

    /** 矩阵行：一个成员的覆盖情况 */
    @Data
    public static class MemberRow {
    /** 成员档案 id */
        private Long memberId;
    /** 成员姓名 */
        private String memberName;
    /** 与户主关系：self/spouse/father/mother/child/other */
        private String relation;
        /** 已配置的险种集合 */
        private List<String> categories;
    }

    /** 该成员名下的保障行列表 */
    private List<MemberRow> rows;

    /** 全家年缴保费合计（趸交不计入） */
    private BigDecimal totalAnnualPremium;

    /** 全家有效保单数 */
    private long activePolicyCount;
}
