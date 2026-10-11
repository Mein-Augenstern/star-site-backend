package com.xp.starsite.family.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭成员（自然人档案，≠ 登录账号）
 */
@Data
@TableName("family_member")
public class FamilyMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long familyId;

    private String name;

    /** self/spouse/father/mother/child/other */
    private String relation;

    /** 0未知 1男 2女 */
    private Integer gender;

    private LocalDate birthDate;

    private String phone;

    /** 头像文件 id（star-site-file） */
    private Long avatarFileId;

    /** 绑定的登录账号 id，可空（老人小孩可能没有账号） */
    private Long linkedMemberId;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
