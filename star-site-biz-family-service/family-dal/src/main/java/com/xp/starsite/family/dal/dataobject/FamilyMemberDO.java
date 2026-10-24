package com.xp.starsite.family.dal.dataobject;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭成员档案数据对象（family_member 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/FamilyMember。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class FamilyMemberDO {

    /** 主键 */
    private Long id;

    /** 所属家庭 id */
    private Long familyId;

    /** 成员姓名 */
    private String name;

    /** 与顶梁柱关系（self/spouse/child/parent/...） */
    private String relation;

    /** 性别（0 未知 1 男 2 女） */
    private Integer gender;

    /** 出生日期 */
    private LocalDate birthDate;

    /** 手机号 */
    private String phone;

    /** 头像文件 id（file 服务） */
    private Long avatarFileId;

    /** 关联登录账号 id（未注册为 null） */
    private Long linkedMemberId;

    /** 备注 */
    private String remark;

    /** 逻辑删标志（0 正常 1 已删） */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
