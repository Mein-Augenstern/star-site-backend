package com.xp.starsite.member.controller;

import com.xp.starsite.api.dto.MemberContactDTO;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.member.controller.actions.MemberActions;
import com.xp.starsite.member.service.MemberService;
import com.xp.starsite.member.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务间内部接口（Feign 提供方）。/internal/** 不经网关，外网 404。
 * <p>
 * 业务逻辑统一经模板包裹：模板包掉成功与失败的全部响应处理（成功 R.ok / 失败 R.fail），
 * Controller 一行返回；动作命名规范 member.internal.*。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/internal/member")
@RequiredArgsConstructor
public class MemberInternalController {

    private final MemberService memberService;
    private final QueryTemplate queryTemplate;

    /**
     * 会员联系方式（提醒邮件触达用；邮箱可空）
     *
     * @param id 会员 id
     * @return 联系方式（会员不存在时 data 为 null）
     */
    @GetMapping("/{id}/contact")
    public R<MemberContactDTO> contact(@PathVariable Long id) {
        return queryTemplate.query(MemberActions.INTERNAL_CONTACT, () -> memberService.getContact(id));
    }
}
