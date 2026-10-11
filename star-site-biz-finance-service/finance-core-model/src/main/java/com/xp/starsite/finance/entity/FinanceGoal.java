package com.xp.starsite.finance.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭储蓄目标（finance_goal）：如 教育金/换车基金；V1 手动更新进度。
 *
 * @author xiaopu
 * @since 2026-10
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 */
@Data
public class FinanceGoal {

    /** 雪花 id（应用侧赋值，与其他 finance 表范式一致） */
    private Long id;

    /** 家庭 id（多人协作预留冗余） */
    private Long familyId;

    /** 归属账号 id（member 服务） */
    private Long ownerMemberId;

    /** 目标名（如 教育金） */
    private String name;

    /** 目标金额 */
    private BigDecimal targetAmount;

    /** 当前已存金额（V1 手动更新） */
    private BigDecimal currentAmount;

    /** 目标截止日（可空） */
    private LocalDate deadline;

    /** 备注 */
    private String remark;

    /** 逻辑删标志（0 正常 1 已删） */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
