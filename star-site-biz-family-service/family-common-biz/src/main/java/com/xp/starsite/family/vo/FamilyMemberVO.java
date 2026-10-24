package com.xp.starsite.family.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭成员视图
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class FamilyMemberVO {

    /** 成员档案 id */
    private Long id;

    /** 所属家庭 id */
    private Long familyId;

    /** 姓名/称呼 */
    private String name;

    /** 与顶梁柱关系：self/spouse/father/mother/child/other */
    private String relation;

    /** 性别：0未知 1男 2女 */
    private Integer gender;

    /** 生日 */
    private LocalDate birthDate;

    /** 联系电话 */
    private String phone;

    /** 头像文件 id（star-site-file） */
    private Long avatarFileId;

    /** 绑定的登录账号 id，可空（老人小孩可能没有账号） */
    private Long linkedMemberId;

    /** 备注（过敏史/慢性病等健康标注） */
    private String remark;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
