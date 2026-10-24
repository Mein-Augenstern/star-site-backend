package com.xp.starsite.family.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭（family）：家庭操作系统的组织根，一个顶梁柱建一个家；
 * 成员档案见 {@link FamilyMember}（自然人 ≠ 登录账号）。
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class Family {

    /** 主键 */
    private Long id;

    /** 家庭名（如 「我们仨」） */
    private String name;

    /** 创建者账号 id（star-site-member 的 member.id），即家庭顶梁柱 */
    private Long ownerMemberId;

    /** 备注 */
    private String remark;

    /** 逻辑删标志（0 正常 1 已删） */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
