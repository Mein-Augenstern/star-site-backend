package com.xp.starsite.member.mapper;

import com.xp.starsite.member.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * member 会员主表数据访问（DAL）：SQL 全部收敛在 mapper XML，业务层只见业务语义方法。
 * 逻辑删：member.deleted，查询一律带 deleted=0。
 * 主键自增：INSERT 不含 id（useGeneratedKeys 回填），created_at/updated_at 由 DB 维护。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface MemberMapper {

    /** 新建会员（username/password 微信静默注册时可为 null） */
    int insert(Member member);

    /** 按 id 取会员（含逻辑删过滤，不存在返回 null） */
    Member selectById(@Param("id") Long id);

    /** 按用户名取会员（登录链路；至多一条，不存在返回 null） */
    Member selectByUsername(@Param("username") String username);

    /** 用户名占用计数（注册查重；与 selectByUsername 同条件 deleted=0） */
    long countByUsername(@Param("username") String username);

    /**
     * 资料更新：仅更新非空字段（nickname/avatar/email/phone/gender/bio，
     * 与原 MyBatis-Plus updateById 的"null 字段不更新"语义一致）。
     * username/password/status 等敏感列不在本语句范围内，密码变更走 {@link #updatePassword}。
     */
    int updateProfile(Member member);

    /** 密码变更（BCrypt 密文整列替换） */
    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
