package com.xp.starsite.finance.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月度分类预算（finance_budget）：按「分类 × 月份」控制支出。
 *
 * @author xiaopu
 * @since 2026-10
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 */
@Data
public class FinanceBudget {

    /** 雪花 id（应用侧赋值，与其他 finance 表范式一致） */
    private Long id;

    /** 家庭 id（多人协作预留冗余） */
    private Long familyId;

    /** 归属账号 id（member 服务） */
    private Long ownerMemberId;

    /** 分类枚举（支出类） */
    private String category;

    /** 预算月份 YYYY-MM */
    private String period;

    /** 预算金额 */
    private BigDecimal amount;

    /** 逻辑删标志（0 正常 1 已删） */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
