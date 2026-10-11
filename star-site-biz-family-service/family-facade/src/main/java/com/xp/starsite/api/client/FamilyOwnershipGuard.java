package com.xp.starsite.api.client;

import com.xp.starsite.api.dto.FamilyMemberSnapshotDTO;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 家庭归属校验门卫：包装 FamilyApi，把 Feign 结果翻译成异常语义。
 *
 * 故意不加 @Component（启动类 scanBasePackages 扫全 starsite 包，
 * family 自己不能注册它）；由需要的业务服务在配置类里 @Bean 引入：
 * <pre>
 * @Bean
 * public FamilyOwnershipGuard familyOwnershipGuard(FamilyApi familyApi) {
 *     return new FamilyOwnershipGuard(familyApi);
 * }
 * </pre>
 *
 * fail-closed：family 服务不可用时拒绝请求，而不是放行。
 */
@Slf4j
@RequiredArgsConstructor
public class FamilyOwnershipGuard {

    private final FamilyApi familyApi;

    /** 校验家庭成员归属当前账号，不归属抛 403 */
    public FamilyMemberSnapshotDTO requireOwned(Long memberId, Long familyMemberId) {
        R<FamilyMemberSnapshotDTO> r = invoke(() -> familyApi.checkOwnership(memberId, familyMemberId));
        if (r.getData() == null) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权限访问该家庭成员的数据");
        }
        return r.getData();
    }

    /** 当前账号家庭的全部成员 id（无家庭返回空列表，用于列表/搜索的范围收口） */
    public List<Long> listOwnedMemberIds(Long memberId) {
        List<FamilyMemberSnapshotDTO> members = invoke(() -> familyApi.listMembers(memberId)).getData();
        return members == null ? List.of()
                : members.stream().map(FamilyMemberSnapshotDTO::getId).toList();
    }

    /** 当前账号家庭的全部成员快照（无家庭返回空列表；保障地图等需要姓名/关系的场景用） */
    public List<FamilyMemberSnapshotDTO> listOwnedMembers(Long memberId) {
        List<FamilyMemberSnapshotDTO> members = invoke(() -> familyApi.listMembers(memberId)).getData();
        return members == null ? List.of() : members;
    }

    private <T> R<T> invoke(FeignCall<T> call) {
        R<T> r;
        try {
            r = call.get();
        } catch (Exception e) {
            log.error("调用 family 服务失败（fail-closed 拒绝请求）: {}", e.getMessage(), e);
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "家庭服务暂不可用，请稍后再试");
        }
        if (r == null || r.getCode() != ResultCode.SUCCESS.getCode()) {
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "家庭服务返回异常，请稍后再试");
        }
        return r;
    }

    @FunctionalInterface
    private interface FeignCall<T> {
        R<T> get();
    }
}
