package com.xp.starsite.member.dal.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员主表数据对象（member 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/Member。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class MemberDO {

    /** 主键 */
    private Long id;

    /** 用户名（微信静默注册时可为 null） */
    private String username;

    /** 密码（BCrypt 密文） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 性别（0 未知 1 男 2 女） */
    private Integer gender;

    /** 个人简介 */
    private String bio;

    /** 状态（1 正常 2 禁用） */
    private Integer status;

    /** 逻辑删标志（0 正常 1 已删） */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
