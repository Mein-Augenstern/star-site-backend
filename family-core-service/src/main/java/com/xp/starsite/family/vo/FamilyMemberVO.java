package com.xp.starsite.family.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭成员视图
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class FamilyMemberVO {

    private Long id;

    private Long familyId;

    private String name;

    private String relation;

    private Integer gender;

    private LocalDate birthDate;

    private String phone;

    private Long avatarFileId;

    private Long linkedMemberId;

    private String remark;

    private LocalDateTime createdAt;
}
