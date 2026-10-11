package com.xp.starsite.member.service;

import com.xp.starsite.member.dto.LoginDTO;
import com.xp.starsite.member.dto.MemberUpdateDTO;
import com.xp.starsite.member.dto.PasswordUpdateDTO;
import com.xp.starsite.member.dto.RegisterDTO;
import com.xp.starsite.member.vo.LoginVO;
import com.xp.starsite.member.vo.MemberVO;

/**
 * 会员服务接口
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface MemberService {

    /**
     * 注册
     *
     * @param dto 注册信息（用户名/密码等）
     * @throws com.xp.starsite.common.exception.BizException 用户名已被注册（4001）
     */
    void register(RegisterDTO dto);

    /**
     * 登录，签发 JWT
     *
     * @param dto 登录信息（用户名/密码）
     * @return 登录视图（token 与会员资料）
     * @throws com.xp.starsite.common.exception.BizException 用户名或密码错误（4002）、账号已被禁用（4003）
     */
    LoginVO login(LoginDTO dto);

    /**
     * 查询个人资料
     *
     * @param id 会员 id
     * @return 会员资料视图
     */
    MemberVO getById(Long id);

    /**
     * 编辑个人资料
     *
     * @param id  会员 id
     * @param dto 待更新的资料字段
     * @return 更新后的会员资料视图
     */
    MemberVO updateProfile(Long id, MemberUpdateDTO dto);

    /**
     * 修改密码
     *
     * @param id  会员 id
     * @param dto 原密码与新密码
     * @throws com.xp.starsite.common.exception.BizException 原密码错误（4004）、
     *                                                       用户名或密码错误（4002，改密前身份复核失败）
     */
    void changePassword(Long id, PasswordUpdateDTO dto);
}
