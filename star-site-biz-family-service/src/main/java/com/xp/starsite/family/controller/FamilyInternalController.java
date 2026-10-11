package com.xp.starsite.family.controller;

import com.xp.starsite.api.dto.FamilyMemberSnapshotDTO;
import com.xp.starsite.common.result.R;
import com.xp.starsite.family.service.FamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 服务间内部接口（Feign 提供方）。
 * <p>
 * /internal/** 不在网关路由表里，外网请求到网关直接 404；
 * JwtAuthInterceptor 只拦 /api/**，本控制器不经 JWT，仅限内网服务间调用。
 * 调用方统一走 star-site-api 的 FamilyApi + FamilyOwnershipGuard。
 */
@RestController
@RequestMapping("/internal/family")
@RequiredArgsConstructor
public class FamilyInternalController {

    private final FamilyService familyService;

    @GetMapping("/ownership")
    public R<FamilyMemberSnapshotDTO> checkOwnership(@RequestParam("memberId") Long memberId,
                                                     @RequestParam("familyMemberId") Long familyMemberId) {
        return R.ok(familyService.checkOwnership(memberId, familyMemberId));
    }

    @GetMapping("/members")
    public R<List<FamilyMemberSnapshotDTO>> listMembers(@RequestParam("memberId") Long memberId) {
        return R.ok(familyService.listMemberSnapshots(memberId));
    }
}
