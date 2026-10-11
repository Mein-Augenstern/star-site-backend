package com.xp.starsite.family.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭成员（family_member）：自然人档案，≠ 登录账号（member）。
 * 老人小孩可以没有账号；有账号的经 linked_member_id 关联。
 * 归属校验（FamilyOwnershipGuard）即以本表的 family_id → owner 关系为准。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@TableName("family_member")
public class FamilyMember {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属家庭 id */
    private Long familyId;

    /** 姓名/称呼 */
    private String name;

    /** 与顶梁柱关系：self/spouse/father/mother/child/other */
    private String relation;

    /** 0未知 1男 2女 */
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

    /** 逻辑删标志（0 正常 1 已删） */
    @TableLogic
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
