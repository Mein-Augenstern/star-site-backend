package com.xp.starsite.family.controller;

import com.xp.starsite.api.dto.FamilyMemberSnapshotDTO;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.family.service.FamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 服务间内部接口（Feign 提供方）：归属校验与成员快照。
 * <p>
 * /internal/** 不在网关路由表里，外网请求到网关直接 404；
 * JwtAuthInterceptor 只拦 /api/**，本控制器不经 JWT，仅限内网服务间调用。
 * 调用方统一走 family-facade 的 FamilyApi + FamilyOwnershipGuard。
 * 业务逻辑统一经模板包裹（动作名 family.internal.*）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/internal/family")
@RequiredArgsConstructor
public class FamilyInternalController {

    private final FamilyService familyService;
    private final QueryTemplate queryTemplate;

    /**
     * 归属校验：归属返回成员快照；不归属/不存在返回 data=null
     *
     * @param memberId       户主账号 id
     * @param familyMemberId 待校验的成员档案 id
     * @return 成员快照，不归属/不存在为 null
     */
    @GetMapping("/ownership")
    public R<FamilyMemberSnapshotDTO> checkOwnership(@RequestParam("memberId") Long memberId,
                                                     @RequestParam("familyMemberId") Long familyMemberId) {
        return queryTemplate.query("family.internal.ownership",
                () -> familyService.checkOwnership(memberId, familyMemberId));
    }

    /**
     * 当前账号家庭的成员快照全集（无家庭返回空列表）
     *
     * @param memberId 户主账号 id
     * @return 成员快照列表
     */
    @GetMapping("/members")
    public R<List<FamilyMemberSnapshotDTO>> listMembers(@RequestParam("memberId") Long memberId) {
        return queryTemplate.query("family.internal.members",
                () -> familyService.listMemberSnapshots(memberId));
    }
}
