package com.xp.starsite.member.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
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
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public R<MemberVO> me(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(memberService.getById(memberId));
    }

    @PutMapping("/me")
    public R<MemberVO> updateMe(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                @Valid @RequestBody MemberUpdateDTO dto) {
        return R.ok(memberService.updateProfile(memberId, dto));
    }

    @PutMapping("/me/password")
    public R<Void> changePassword(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                  @Valid @RequestBody PasswordUpdateDTO dto) {
        memberService.changePassword(memberId, dto);
        return R.ok();
    }
}
