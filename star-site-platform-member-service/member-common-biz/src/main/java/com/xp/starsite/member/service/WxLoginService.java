package com.xp.starsite.member.service;

import com.xp.starsite.member.vo.LoginVO;

/**
 * 微信登录/绑定用例接口（app 层）。
 * 与账密登录（MemberService）同构：都返回签发 JWT 的 LoginVO。
 *
 * @author muyi
 * @since 2026-10
 */
public interface WxLoginService {

    /**
     * 小程序登录：静默注册/登录，返回与账号密码登录同构的 LoginVO
     *
     * @param code 小程序 wx.login 的 code（用于换取 openid）
     * @return 登录视图（token 与会员资料）
     * @throws com.xp.starsite.common.exception.BizException code 为空（400）、账号已被禁用（403）
     */
    LoginVO loginByWechatMp(String code);

    /**
     * 账密绑定微信身份：校验账密后将当前微信 openid（WECHAT_MP 身份）挂到指定账号。
     * 身份已被其他账号占用时报错；绑定成功返回目标账号的 LoginVO。
     *
     * @param code     当前微信会话的 code（用于换取 openid）
     * @param username 目标账号
     * @param password 目标账号密码
     * @return 目标账号的登录视图（token 与会员资料）
     * @throws com.xp.starsite.common.exception.BizException code 为空（400）、账号或密码错误（400）、
     *                                                       账号已被禁用（403）、该微信已绑定其他账号（400）
     */
    LoginVO bindAccountByPassword(String code, String username, String password);
}
