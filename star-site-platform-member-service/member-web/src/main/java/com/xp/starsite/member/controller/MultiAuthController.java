package com.xp.starsite.member.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.member.controller.actions.MemberActions;
import com.xp.starsite.member.dto.ProfileCompleteDTO;
import com.xp.starsite.member.service.MemberService;
import com.xp.starsite.member.service.WxLoginService;
import com.xp.starsite.member.vo.IdentityVO;
import com.xp.starsite.member.vo.LoginVO;
import com.xp.starsite.member.vo.MemberVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 多端认证接口：wx-login 免 token；identities/bind/profile 需 JWT。
 *
 * <p>会员域多登录方式入口：微信静默登录/注册、账密绑定微信、登录方式列表与解绑
 * （至少保留一种防自锁）、资料完善、邮箱验证码绑定（开发态验证码走内存+日志）。
 * 校验与装配逻辑全部在 MemberService/WxLoginService 业务方法内，Controller 只透传参数。
 * <p>
 * 业务逻辑统一经模板包裹：模板包掉成功与失败的全部响应处理（成功 R.ok / 失败 R.fail），
 * Controller 一行返回；动作命名规范 member.auth.* / member.bind.* / member.profile.*。
 *
 * @author muyi
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MultiAuthController {

    private final WxLoginService wxLoginService;
    private final MemberService memberService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 微信小程序登录：静默注册/登录（免 token）
     *
     * @param body 登录入参（wx.login 的 code）
     * @return 登录视图（JWT、有效期与会员摘要）
     */
    @PostMapping("/wx-login")
    public R<LoginVO> wxLogin(@RequestBody Map<String, String> body) {
        return writeTemplate.write(MemberActions.AUTH_WX_LOGIN,
                () -> wxLoginService.loginByWechatMp(body.get("code")));
    }

    /**
     * 账密绑定微信身份：将当前微信 openid 挂到指定账号（多端同一份数据）
     *
     * @param body 绑定入参（code/username/password）
     * @return 绑定后重新签发的登录视图
     */
    @PostMapping("/bind/account")
    public R<LoginVO> bindAccount(@RequestBody Map<String, String> body) {
        return writeTemplate.write(MemberActions.BIND_ACCOUNT, () -> wxLoginService.bindAccountByPassword(
                body.get("code"), body.get("username"), body.get("password")));
    }

    /**
     * 我的登录方式列表（标识符脱敏展示）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @return 登录方式视图列表（id/类型/脱敏标识/绑定时间）
     */
    @GetMapping("/identities")
    public R<List<IdentityVO>> identities(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query(MemberActions.BIND_LIST, () -> memberService.listIdentities(memberId));
    }

    /**
     * 解绑登录方式（至少保留一种，防自锁）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param id       绑定关系 id
     * @return 解绑成功（无业务数据）
     */
    @DeleteMapping("/identities/{id}")
    public R<Void> unbind(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        return writeTemplate.write(MemberActions.BIND_UNBIND,
                () -> memberService.unbindIdentity(memberId, id));
    }

    /**
     * 完善昵称/头像（微信静默注册后引导补充）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param dto      资料完善内容（昵称/头像，可选字段）
     * @return 完善后的会员资料视图
     */
    @PutMapping("/profile")
    public R<MemberVO> completeProfile(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                       @Valid @RequestBody ProfileCompleteDTO dto) {
        return writeTemplate.write(MemberActions.PROFILE_COMPLETE,
                () -> memberService.completeProfile(memberId, dto));
    }

    /**
     * 发送邮箱验证码（M1：SMTP 未配置时验证码仍有效，从日志取；验证码存内存，生产换 Redis）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param body     入参（email）
     * @return 发送受理（无业务数据）
     */
    @PostMapping("/bind/email-code")
    public R<Void> sendEmailCode(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                 @RequestBody Map<String, String> body) {
        return writeTemplate.write(MemberActions.BIND_EMAIL_CODE_SEND,
                () -> memberService.sendEmailCode(memberId, body.get("email")));
    }

    /**
     * 邮箱验证码绑定（校验验证码后落 EMAIL 身份；账号邮箱为空则回填）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param body     入参（email/code）
     * @return 绑定成功（无业务数据）
     */
    @PostMapping("/bind/email")
    public R<Void> bindEmail(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                             @RequestBody Map<String, String> body) {
        return writeTemplate.write(MemberActions.BIND_EMAIL,
                () -> memberService.bindEmail(memberId, body.get("email"), body.get("code")));
    }
}
