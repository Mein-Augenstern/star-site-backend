package com.xp.starsite.member.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xp.starsite.common.async.AsyncCommand;
import com.xp.starsite.common.async.mapper.AsyncCommandMapper;
import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * star-async 异步命令管控端（管理员运维：卡单/重试/监控）。
 * 鉴权：JWT 必过网关；可选 ADMIN_MEMBER_IDS 白名单（逗号分隔，留空=开发态放行所有登录用户）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/admin/async")
@RequiredArgsConstructor
public class AdminAsyncController {

    private final AsyncCommandMapper mapper;

    @Value("${admin.member-ids:}")
    private String adminMemberIds;

    /** 管理员白名单校验（未配置=放行，本地开发态） */
    private void requireAdmin(Long memberId) {
        if (adminMemberIds == null || adminMemberIds.isBlank()) {
            return;
        }
        for (String id : adminMemberIds.split(",")) {
            if (String.valueOf(memberId).equals(id.trim())) {
                return;
            }
        }
        throw new BizException(403, "无管理员权限");
    }

    /** 监控总览：各状态计数 + 积压 + 近期死信 */
    @GetMapping("/summary")
    public R<Map<String, Object>> summary(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        requireAdmin(memberId);
        Map<String, Object> out = new HashMap<>();
        for (String st : List.of("PENDING", "RUNNING", "SUCCESS", "DEAD")) {
            out.put(st.toLowerCase(), mapper.selectCount(
                    new LambdaQueryWrapper<AsyncCommand>().eq(AsyncCommand::getStatus, st)));
        }
        out.put("backlogDue", mapper.selectCount(new LambdaQueryWrapper<AsyncCommand>()
                .eq(AsyncCommand::getStatus, "PENDING")
                .le(AsyncCommand::getNextRunAt, LocalDateTime.now())));
        // 按类型聚合（近 500 条内的分布，够运维肉眼判断）
        Map<String, Long> byType = new HashMap<>();
        for (AsyncCommand c : mapper.selectList(new LambdaQueryWrapper<AsyncCommand>()
                .orderByDesc(AsyncCommand::getId).last("LIMIT 500"))) {
            byType.merge(c.getCmdType(), 1L, Long::sum);
        }
        out.put("byType", byType);
        return R.ok(out);
    }

    /** 分页列表（status/type 可选过滤） */
    @GetMapping("/list")
    public R<Map<String, Object>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) String cmdType,
                                       @RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "20") long size) {
        requireAdmin(memberId);
        LambdaQueryWrapper<AsyncCommand> w = new LambdaQueryWrapper<AsyncCommand>()
                .eq(status != null && !status.isBlank(), AsyncCommand::getStatus, status)
                .eq(cmdType != null && !cmdType.isBlank(), AsyncCommand::getCmdType, cmdType)
                .orderByDesc(AsyncCommand::getId);
        long total = mapper.selectCount(w);
        List<AsyncCommand> records = mapper.selectList(w.last("LIMIT " + ((page - 1) * size) + "," + size));
        Map<String, Object> out = new HashMap<>();
        out.put("records", records);
        out.put("total", total);
        out.put("page", page);
        out.put("size", size);
        return R.ok(out);
    }

    /** 手动重试：DEAD/PENDING 重置为待执行（清零重试计数，立即执行） */
    @PostMapping("/{id}/retry")
    public R<Void> retry(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId, @PathVariable Long id) {
        requireAdmin(memberId);
        AsyncCommand cmd = requireCmd(id);
        if ("RUNNING".equals(cmd.getStatus())) {
            throw new BizException(400, "任务执行中，请先用「重占」收回再重试");
        }
        if ("SUCCESS".equals(cmd.getStatus())) {
            throw new BizException(400, "任务已成功，无需重试");
        }
        cmd.setStatus("PENDING");
        cmd.setRetryCount(0);
        cmd.setNextRunAt(LocalDateTime.now());
        cmd.setClaimToken(null);
        mapper.updateById(cmd);
        return R.ok();
    }

    /** 重占：卡单处理——把卡死的 RUNNING 强制放回队列（先确认执行器已死再操作） */
    @PostMapping("/{id}/reclaim")
    public R<Void> reclaim(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId, @PathVariable Long id) {
        requireAdmin(memberId);
        AsyncCommand cmd = requireCmd(id);
        if (!"RUNNING".equals(cmd.getStatus())) {
            throw new BizException(400, "仅 RUNNING 状态可重占");
        }
        cmd.setStatus("PENDING");
        cmd.setClaimToken(null);
        cmd.setNextRunAt(LocalDateTime.now());
        mapper.updateById(cmd);
        return R.ok();
    }

    /** 终止：卡单或待执行任务直接置死信（不再重试） */
    @PostMapping("/{id}/kill")
    public R<Void> kill(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId, @PathVariable Long id) {
        requireAdmin(memberId);
        AsyncCommand cmd = requireCmd(id);
        if ("SUCCESS".equals(cmd.getStatus())) {
            throw new BizException(400, "已成功任务不可终止");
        }
        cmd.setStatus("DEAD");
        cmd.setClaimToken(null);
        cmd.setLastError("MANUAL_KILL by member " + memberId);
        mapper.updateById(cmd);
        return R.ok();
    }

    private AsyncCommand requireCmd(Long id) {
        AsyncCommand cmd = mapper.selectById(id);
        if (cmd == null) {
            throw new BizException(404, "命令不存在");
        }
        return cmd;
    }
}
