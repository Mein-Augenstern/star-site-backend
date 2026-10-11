package com.xp.starsite.member.controller;

import com.xp.starsite.api.dto.MemberContactDTO;
import com.xp.starsite.common.result.R;
import com.xp.starsite.member.service.MemberService;
import com.xp.starsite.member.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务间内部接口（Feign 提供方）。/internal/** 不经网关，外网 404。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/internal/member")
@RequiredArgsConstructor
public class MemberInternalController {

    private final MemberService memberService;

    /** 会员联系方式（提醒邮件触达用；邮箱可空） */
    @GetMapping("/{id}/contact")
    public R<MemberContactDTO> contact(@PathVariable Long id) {
        MemberVO member = memberService.getById(id);
        if (member == null) {
            return R.ok(null);
        }
        MemberContactDTO dto = new MemberContactDTO();
        BeanUtils.copyProperties(member, dto);
        return R.ok(dto);
    }
}
