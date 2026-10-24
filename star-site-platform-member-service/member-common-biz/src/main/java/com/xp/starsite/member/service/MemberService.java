package com.xp.starsite.member.service;

import com.xp.starsite.member.dto.LoginDTO;
import com.xp.starsite.member.dto.MemberUpdateDTO;
import com.xp.starsite.member.dto.PasswordUpdateDTO;
import com.xp.starsite.member.dto.ProfileCompleteDTO;
import com.xp.starsite.member.dto.RegisterDTO;
import com.xp.starsite.member.vo.IdentityVO;
import com.xp.starsite.member.vo.LoginVO;
import com.xp.starsite.member.vo.MemberVO;

import java.util.List;

/**
 * 会员服务接口（账号/资料/多端登录身份管理）
 *
 * @author muyi
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
     * 会员联系方式（内部契约出参：facade DTO；提醒邮件触达用，邮箱可空）
     *
     * @param id 会员 id
     * @return 联系方式；会员不存在返回 null
     */
    com.xp.starsite.api.dto.MemberContactDTO getContact(Long id);

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

    /**
     * 我的登录方式列表（标识符脱敏展示）
     *
     * @param memberId 会员 id
     * @return 登录方式视图列表（id/类型/脱敏标识/绑定时间）
     */
    List<IdentityVO> listIdentities(Long memberId);

    /**
     * 解绑登录方式（至少保留一种，防自锁）
     *
     * @param memberId 会员 id（归属校验）
     * @param id       绑定关系 id
     * @throws com.xp.starsite.common.exception.BizException 绑定关系不存在（404）、
     *                                                       至少保留一种登录方式无法解绑（400）
     */
    void unbindIdentity(Long memberId, Long id);

    /**
     * 完善昵称/头像（微信静默注册后引导补充；仅非空字段生效）
     *
     * @param memberId 会员 id
     * @param dto      资料完善内容（昵称/头像，可选字段）
     * @return 完善后的会员资料视图
     * @throws com.xp.starsite.common.exception.BizException 账号不存在（404）
     */
    MemberVO completeProfile(Long memberId, ProfileCompleteDTO dto);

    /**
     * 发送邮箱验证码（M1：SMTP 未配置时验证码仍有效，从日志取；验证码存内存，生产换 Redis）
     *
     * @param memberId 会员 id（验证码按账号隔离存储）
     * @param email    待绑定邮箱
     * @throws com.xp.starsite.common.exception.BizException 邮箱格式不正确（400）、该邮箱已被其他账号绑定（400）
     */
    void sendEmailCode(Long memberId, String email);

    /**
     * 邮箱验证码绑定（校验验证码后落 EMAIL 身份；账号邮箱为空则回填）
     *
     * @param memberId 会员 id
     * @param email    待绑定邮箱
     * @param code     邮箱验证码
     * @throws com.xp.starsite.common.exception.BizException 验证码错误或已过期（400）、
     *                                                       该邮箱已被其他账号绑定（400）
     */
    void bindEmail(Long memberId, String email, String code);
}
