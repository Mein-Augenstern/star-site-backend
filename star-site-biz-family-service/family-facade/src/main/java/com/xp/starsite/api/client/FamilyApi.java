package com.xp.starsite.api.client;

import com.xp.starsite.api.dto.FamilyMemberSnapshotDTO;
import com.xp.starsite.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * family 服务内部接口（Feign 契约）。
 * 服务端实现见 family 服务 FamilyInternalController；调用方须经
 * FamilyOwnershipGuard 使用，不要直接调本接口。
 *
 * /internal/** 不经网关路由（外网 404），仅限内网服务间调用。
 *
 * @author xiaopu
 * @since 2026-10
 */
@FeignClient(name = "star-site-biz-family", contextId = "familyInternalApi", path = "/internal/family")
public interface FamilyApi {

    /**
     * 校验家庭成员是否归属当前账号（家庭 owner 校验）
     *
     * @return 归属则返回成员快照；不归属/不存在返回 data=null（code=0）
     */
    @GetMapping("/ownership")
    R<FamilyMemberSnapshotDTO> checkOwnership(@RequestParam("memberId") Long memberId,
                                              @RequestParam("familyMemberId") Long familyMemberId);

    /**
     * 当前账号家庭的全量成员快照（无家庭返回空列表）
     */
    @GetMapping("/members")
    R<List<FamilyMemberSnapshotDTO>> listMembers(@RequestParam("memberId") Long memberId);
}
