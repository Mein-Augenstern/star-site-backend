package com.xp.starsite.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员表实体（created_at / updated_at 由数据库默认值维护）
 */
@Data
@TableName("member")
public class Member {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名 */
    private String username;

    /** BCrypt 密文 */
    private String password;

    private String nickname;

    /** 头像 URL */
    private String avatar;

    private String email;

    private String phone;

    /** 0未知 1男 2女 */
    private Integer gender;

    private String bio;

    /** 1正常 0禁用 */
    private Integer status;

    /** 逻辑删除：1已删 0未删 */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
