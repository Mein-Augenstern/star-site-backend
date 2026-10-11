package com.xp.starsite.member.controller;

import com.xp.starsite.common.result.R;
import com.xp.starsite.member.dto.LoginDTO;
import com.xp.starsite.member.dto.RegisterDTO;
import com.xp.starsite.member.service.MemberService;
import com.xp.starsite.member.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（免 token）
 *
 * <p>会员域 REST 入口：提供账号注册与账密登录，登录成功签发 JWT，供小程序/Web 前端调用。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterDTO dto) {
        memberService.register(dto);
        return R.ok();
    }

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(memberService.login(dto));
    }
}
