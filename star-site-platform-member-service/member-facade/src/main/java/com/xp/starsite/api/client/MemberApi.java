package com.xp.starsite.api.client;

import com.xp.starsite.api.dto.MemberContactDTO;
import com.xp.starsite.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * member 服务内部接口（Feign 契约）：提醒触达取联系方式。
 * /internal/** 不经网关路由，仅限内网服务间调用。
 */
@FeignClient(name = "star-site-platform-member", contextId = "memberInternalApi", path = "/internal/member")
public interface MemberApi {

    /** 会员联系方式（昵称/邮箱；邮箱可能为空） */
    @GetMapping("/{id}/contact")
    R<MemberContactDTO> contact(@PathVariable("id") Long memberId);
}
