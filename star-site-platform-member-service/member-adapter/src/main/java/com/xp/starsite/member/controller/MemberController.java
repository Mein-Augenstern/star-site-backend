package com.xp.starsite.member.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.member.dto.MemberUpdateDTO;
import com.xp.starsite.member.dto.PasswordUpdateDTO;
import com.xp.starsite.member.service.MemberService;
import com.xp.starsite.member.vo.MemberVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员资料接口（需 JWT，由 JwtInterceptor 拦截）
 * <p>
 * 业务逻辑统一经模板包裹：模板包掉成功与失败的全部响应处理（成功 R.ok / 失败 R.fail），
 * Controller 一行返回；动作命名规范 member.profile.*。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 我的资料
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @return 会员资料视图
     */
    @GetMapping("/me")
    public R<MemberVO> me(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query("member.profile.get", () -> memberService.getById(memberId));
    }

    /**
     * 修改我的资料（昵称/头像等，仅更新传入字段）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param dto      资料更新内容
     * @return 更新后的会员资料视图
     */
    @PutMapping("/me")
    public R<MemberVO> updateMe(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                @Valid @RequestBody MemberUpdateDTO dto) {
        return writeTemplate.write("member.profile.update",
                () -> memberService.updateProfile(memberId, dto));
    }

    /**
     * 修改登录密码（校验旧密码）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param dto      密码修改内容（旧密码/新密码）
     * @return 修改成功（无业务数据）
     */
    @PutMapping("/me/password")
    public R<Void> changePassword(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                  @Valid @RequestBody PasswordUpdateDTO dto) {
        return writeTemplate.write("member.profile.changePassword",
                () -> memberService.changePassword(memberId, dto));
    }
}
