package com.xp.starsite.member.controller;

import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
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
 * <p>
 * 业务逻辑统一经模板包裹：模板包掉成功与失败的全部响应处理（成功 R.ok / 失败 R.fail），
 * Controller 一行返回；动作命名规范 member.auth.*。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 账号注册（账密；用户名唯一，昵称缺省取用户名）
     *
     * @param dto 注册信息（用户名/密码/可选昵称）
     * @return 注册成功（无业务数据）
     */
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterDTO dto) {
        return writeTemplate.write("member.auth.register", () -> memberService.register(dto));
    }

    /**
     * 账密登录（账号停用即拒绝；成功签发 JWT）
     *
     * @param dto 登录信息（用户名/密码）
     * @return 登录视图（JWT、有效期与会员摘要）
     */
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return queryTemplate.query("member.auth.login", () -> memberService.login(dto));
    }
}
