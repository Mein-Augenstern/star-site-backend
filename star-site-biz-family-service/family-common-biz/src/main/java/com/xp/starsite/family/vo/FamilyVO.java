package com.xp.starsite.family.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭视图
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class FamilyVO {

    /** 家庭 id */
    private Long id;

    /** 家庭名（如 「我们仨」） */
    private String name;

    /** 备注 */
    private String remark;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
