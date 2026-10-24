package com.xp.starsite.common.async;

import com.xp.starsite.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * star-async 管控服务（2026-10-15 自 member 收敛入组件）：管理员运维的编排与白名单校验。
 * <p>
 * 管理员角色来源 = star.async.admin-member-ids 白名单（member 表无 role 列）；
 * 未配置=放行（本地开发态）。JWT 有效性由网关保证，白名单即本端点最后的角色防线
 * （组件化后不再回查 member 库核验账号存活，暴露窗口以 JWT 有效期为上限）。
 * <p>
 * 表 async_command 归本组件所有（每服务独立 DB 后，各服务自持自己的 async_command 表），
 * 查询/处置经 {@link AsyncCommandService}；白名单经构造注入（对象由
 * AsyncCommandConfig 的 @Bean 工厂创建，@Value 对 new 对象不生效）。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
public class AsyncCommandAdminService {

    /** 管理员白名单（逗号分隔 memberId；未配置=放行，本地开发态） */
    private final String adminMemberIds;

    private final AsyncCommandService commandService;

    public AsyncCommandAdminService(AsyncCommandService commandService, String adminMemberIds) {
        this.commandService = commandService;
        this.adminMemberIds = adminMemberIds == null ? "" : adminMemberIds;
    }

    /**
     * 监控总览：各状态计数 + 积压 + 近期命令按类型分布
     *
     * @param memberId 当前登录账号（过白名单校验）
     * @return 各状态计数（pending/running/success/dead）、积压数 backlogDue 与近 500 条按类型分布 byType
     */
    public Map<String, Object> summary(long memberId) {
        requireAdmin(memberId);
        Map<String, Object> out = new LinkedHashMap<>();
        for (String st : List.of("PENDING", "RUNNING", "SUCCESS", "DEAD")) {
            out.put(st.toLowerCase(), commandService.countByStatus(st));
        }
        out.put("backlogDue", commandService.countBacklogDue(LocalDateTime.now()));
        Map<String, Long> byType = new LinkedHashMap<>();
        for (AsyncCommand c : commandService.selectRecent(500)) {
            byType.merge(c.getCmdType(), 1L, Long::sum);
        }
        out.put("byType", byType);
        return out;
    }

    /**
     * 管控分页列表（status/type 可选过滤）
     *
     * @param memberId 当前登录账号（过白名单校验）
     * @param status   可选，按命令状态过滤
     * @param cmdType  可选，按命令类型过滤
     * @param page     页码（从 1 起）
     * @param size     每页条数
     * @return 分页结果（records/total/page/size）
     */
    public Map<String, Object> list(long memberId, String status, String cmdType, long page, long size) {
        requireAdmin(memberId);
        long total = commandService.countPage(status, cmdType);
        List<AsyncCommand> records = total == 0
                ? List.of()
                : commandService.selectPage(status, cmdType, (page - 1) * size, size);
        return Map.of("records", records, "total", total, "page", page, "size", size);
    }

    /**
     * 手动重试：DEAD/PENDING 重置为待执行（清零重试计数，立即执行）
     *
     * @throws BizException 命令不存在（404）、任务执行中需先重占（400）、已成功任务无需重试（400）、无管理员权限（403）
     */
    public void retry(long memberId, long id) {
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
        commandService.updateState(cmd);
    }

    /**
     * 重占：卡单处理——把卡死的 RUNNING 强制放回队列（先确认执行器已死再操作）
     *
     * @throws BizException 命令不存在（404）、仅 RUNNING 可重占（400）、无管理员权限（403）
     */
    public void reclaim(long memberId, long id) {
        requireAdmin(memberId);
        AsyncCommand cmd = requireCmd(id);
        if (!"RUNNING".equals(cmd.getStatus())) {
            throw new BizException(400, "仅 RUNNING 状态可重占");
        }
        cmd.setStatus("PENDING");
        cmd.setClaimToken(null);
        cmd.setNextRunAt(LocalDateTime.now());
        commandService.updateState(cmd);
    }

    /**
     * 终止：卡单或待执行任务直接置死信（不再重试）
     *
     * @throws BizException 命令不存在（404）、已成功任务不可终止（400）、无管理员权限（403）
     */
    public void kill(long memberId, long id) {
        requireAdmin(memberId);
        AsyncCommand cmd = requireCmd(id);
        if ("SUCCESS".equals(cmd.getStatus())) {
            throw new BizException(400, "已成功任务不可终止");
        }
        cmd.setStatus("DEAD");
        cmd.setClaimToken(null);
        cmd.setLastError("MANUAL_KILL by member " + memberId);
        commandService.updateState(cmd);
    }

    /** 管理员白名单校验（未配置=放行，本地开发态；命中即放行） */
    private void requireAdmin(long memberId) {
        if (adminMemberIds.isBlank()) {
            return;
        }
        for (String id : adminMemberIds.split(",")) {
            if (String.valueOf(memberId).equals(id.trim())) {
                return;
            }
        }
        throw new BizException(403, "无管理员权限");
    }

    private AsyncCommand requireCmd(long id) {
        AsyncCommand cmd = commandService.findById(id);
        if (cmd == null) {
            throw new BizException(404, "异步命令不存在");
        }
        return cmd;
    }
}
