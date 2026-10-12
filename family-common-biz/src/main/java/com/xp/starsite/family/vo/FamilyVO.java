package com.xp.starsite.family.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭视图
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class FamilyVO {

    private Long id;

    private String name;

    private String remark;

    private LocalDateTime createdAt;
}
