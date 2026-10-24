package com.xp.starsite.api.client;

import com.xp.starsite.api.dto.FamilyMemberSnapshotDTO;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.common.rpc.RpcCallGuard;
import com.xp.starsite.common.rpc.RpcException;
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
 * 调用经 {@link RpcCallGuard} 统一三段式处理（判 code/可重试判定/标准异常翻译），
 * 对外仍抛 BizException（历史语义：403 无权限 / 500 依赖不可用），消费方零改动。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@RequiredArgsConstructor
public class FamilyOwnershipGuard {

    private final FamilyApi familyApi;

    /** 无状态，实例化即可用（与各服务 @Bean 单参构造兼容） */
    private final RpcCallGuard rpc = new RpcCallGuard();

    /** 校验家庭成员归属当前账号，不归属抛 403 */
    public FamilyMemberSnapshotDTO requireOwned(Long memberId, Long familyMemberId) {
        FamilyMemberSnapshotDTO member = invoke("family.ownership",
                () -> familyApi.checkOwnership(memberId, familyMemberId));
        if (member == null) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权限访问该家庭成员的数据");
        }
        return member;
    }

    /** 当前账号家庭的全部成员 id（无家庭返回空列表，用于列表/搜索的范围收口） */
    public List<Long> listOwnedMemberIds(Long memberId) {
        List<FamilyMemberSnapshotDTO> members = invoke("family.members",
                () -> familyApi.listMembers(memberId));
        return members == null ? List.of()
                : members.stream().map(FamilyMemberSnapshotDTO::getId).toList();
    }

    /** 当前账号家庭的全部成员快照（无家庭返回空列表；保障地图等需要姓名/关系的场景用） */
    public List<FamilyMemberSnapshotDTO> listOwnedMembers(Long memberId) {
        List<FamilyMemberSnapshotDTO> members = invoke("family.members",
                () -> familyApi.listMembers(memberId));
        return members == null ? List.of() : members;
    }

    /** 统一经 RpcCallGuard fail-closed（code!=0 已被翻译成异常，返回即 data）；RpcException 回译为历史 BizException 语义（500 家庭服务暂不可用） */
    private <T> T invoke(String action, FeignCall<T> call) {
        try {
            return rpc.call(action, () -> call.get());
        } catch (RpcException e) {
            log.error("调用 family 服务失败（fail-closed 拒绝请求）: action={}, retryable={}", action, e.isRetryable(), e);
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "家庭服务暂不可用，请稍后再试");
        }
    }

    @FunctionalInterface
    private interface FeignCall<T> {
        /** 执行 Feign 调用返回原始 R（异常语义由 invoke 统一翻译，fail-closed） */
        R<T> get();
    }
}
