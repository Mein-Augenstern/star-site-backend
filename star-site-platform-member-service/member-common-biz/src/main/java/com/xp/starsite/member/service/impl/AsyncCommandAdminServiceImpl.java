package com.xp.starsite.member.service.impl;

import com.xp.starsite.common.async.AsyncCommand;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.member.constant.MemberStatus;
import com.xp.starsite.member.entity.Member;
import com.xp.starsite.member.repository.AsyncCommandAdminRepository;
import com.xp.starsite.member.repository.MemberRepository;
import com.xp.starsite.member.service.AsyncCommandAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 异步命令管控实现（管理员运维）。
 * 管控查询/处置经仓储层（{@link AsyncCommandAdminRepository}，实现位于 core-service、底层调用 dal DAO）；
 * 执行链路仍走 common 的 AsyncCommandMapper。
 * 管理员校验：ADMIN_MEMBER_IDS 白名单为角色来源（member 表无 role 列），
 * 白名单命中后经 {@link MemberRepository} 核验账号仍存在且未禁用——防已注销/禁用账号凭残留 JWT 冒用管理员身份；
 * 白名单未配置=开发态放行所有登录用户（与原控制器语义一致）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Service
@RequiredArgsConstructor
public class AsyncCommandAdminServiceImpl implements AsyncCommandAdminService {

    private final AsyncCommandAdminRepository adminRepository;
    private final MemberRepository memberRepository;

    @Value("${admin.member-ids:}")
    private String adminMemberIds;

    @Override
    public Map<String, Object> summary(Long memberId) {
        requireAdmin(memberId);
        Map<String, Object> out = new HashMap<>();
        for (String st : List.of("PENDING", "RUNNING", "SUCCESS", "DEAD")) {
            out.put(st.toLowerCase(), adminRepository.countByStatus(st));
        }
        out.put("backlogDue", adminRepository.countBacklogDue(LocalDateTime.now()));
        // 按类型聚合（近 500 条内的分布，够运维肉眼判断）
        Map<String, Long> byType = new HashMap<>();
        for (AsyncCommand c : adminRepository.recent(500)) {
            byType.merge(c.getCmdType(), 1L, Long::sum);
        }
        out.put("byType", byType);
        return out;
    }

    @Override
    public Map<String, Object> list(Long memberId, String status, String cmdType, long page, long size) {
        requireAdmin(memberId);
        // 分页 count 先行，空结果不打查询
        long total = adminRepository.countPage(status, cmdType);
        List<AsyncCommand> records = total == 0 ? List.of()
                : adminRepository.page(status, cmdType, (page - 1) * size, size);
        Map<String, Object> out = new HashMap<>();
        out.put("records", records);
        out.put("total", total);
        out.put("page", page);
        out.put("size", size);
        return out;
    }

    @Override
    public void retry(Long memberId, Long id) {
        requireAdmin(memberId);
        AsyncCommand cmd = requireCmd(id);
        if ("RUNNING".equals(cmd.getStatus())) {
            throw new BizException(400, "任务执行中，请先用「重占」收回再重试");
        }
        if ("SUCCESS".equals(cmd.getStatus())) {
            throw new BizException(400, "已成功任务，无需重试");
        }
        cmd.setStatus("PENDING");
        cmd.setRetryCount(0);
        cmd.setNextRunAt(LocalDateTime.now());
        cmd.setClaimToken(null);
        adminRepository.updateState(cmd);
    }

    @Override
    public void reclaim(Long memberId, Long id) {
        requireAdmin(memberId);
        AsyncCommand cmd = requireCmd(id);
        if (!"RUNNING".equals(cmd.getStatus())) {
            throw new BizException(400, "仅 RUNNING 状态可重占");
        }
        cmd.setStatus("PENDING");
        cmd.setClaimToken(null);
        cmd.setNextRunAt(LocalDateTime.now());
        adminRepository.updateState(cmd);
    }

    @Override
    public void kill(Long memberId, Long id) {
        requireAdmin(memberId);
        AsyncCommand cmd = requireCmd(id);
        if ("SUCCESS".equals(cmd.getStatus())) {
            throw new BizException(400, "已成功任务不可终止");
        }
        cmd.setStatus("DEAD");
        cmd.setClaimToken(null);
        cmd.setLastError("MANUAL_KILL by member " + memberId);
        adminRepository.updateState(cmd);
    }

    /** 管理员白名单校验（未配置=放行，本地开发态；命中后核验账号角色/状态） */
    private void requireAdmin(Long memberId) {
        if (adminMemberIds == null || adminMemberIds.isBlank()) {
            return;
        }
        for (String id : adminMemberIds.split(",")) {
            if (String.valueOf(memberId).equals(id.trim())) {
                // 白名单命中 → 查角色：账号必须仍存在且未禁用（防注销/禁用账号残留 JWT 越权）
                Member member = memberRepository.findById(memberId).orElse(null);
                if (member == null || member.getStatus() == null
                        || member.getStatus() != MemberStatus.ACTIVE) {
                    throw new BizException(403, "无管理员权限");
                }
                return;
            }
        }
        throw new BizException(403, "无管理员权限");
    }

    private AsyncCommand requireCmd(Long id) {
        AsyncCommand cmd = adminRepository.findById(id).orElse(null);
        if (cmd == null) {
            throw new BizException(404, "命令不存在");
        }
        return cmd;
    }
}
