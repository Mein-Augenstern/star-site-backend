package com.xp.starsite.health.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 孕期档案视图：孕周中枢 + 产检计划 + 体重参考
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class PregnancyVO {

    /** 家庭成员 id */
    private Long familyMemberId;

    /** 末次月经第一天 */
    private LocalDate lmpDate;

    /** 预产期（由末次月经推算） */
    private LocalDate dueDate;

    /** 孕X周 */
    private Integer gestationalWeek;

    /** +Y 天 */
    private Integer gestationalDay;

    /** 早期(≤12+6) / 中期(13-27+6) / 晚期(≥28) */
    private String stage;

    /** 距预产期天数（负数=已过预产期） */
    private Long daysToDue;

    /** 孕前 BMI（缺身高/体重为 null） */
    private BigDecimal preBmi;

    /** 全孕期推荐增重区间（kg，IOM 单胎标准；null=信息不足） */
    private String weightGainAdvice;

    /** 产检/关键节点计划 */
    private List<Milestone> milestones;

    @Data
    public static class Milestone {
        /** 里程碑名称（如 NT 检查、大排畸） */
        private String name;

        /** 起始孕周 */
        private Integer fromWeek;

        /** 结束孕周 */
        private Integer toWeek;

        /** pending 未到窗口 / current 窗口期内 / done 已过窗口 */
        private String status;

        /** 提示文案 */
        private String tip;
    }
}
