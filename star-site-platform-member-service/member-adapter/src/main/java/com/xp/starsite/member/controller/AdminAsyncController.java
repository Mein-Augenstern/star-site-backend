package com.xp.starsite.member.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.member.controller.actions.MemberActions;
import com.xp.starsite.member.service.AsyncCommandAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * star-async 异步命令管控端（管理员运维：卡单/重试/监控）。
 * 鉴权：JWT 必过网关；管理员校验（ADMIN_MEMBER_IDS 白名单 + 账号核验）在
 * {@link AsyncCommandAdminService} 内完成，Controller 只透传 memberId 与端点参数。
 * <p>
 * 业务逻辑统一经模板包裹：模板包掉成功与失败的全部响应处理（成功 R.ok / 失败 R.fail），
 * Controller 一行返回；动作命名规范 member.admin.async.*。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/admin/async")
@RequiredArgsConstructor
public class AdminAsyncController {

    private final AsyncCommandAdminService adminAsyncService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 监控总览：各状态计数 + 积压 + 近期死信
     *
     * @param memberId 当前登录账号（JWT 注入，过管理员校验）
     * @return 各状态计数、积压数与近 500 条按类型分布
     */
    @GetMapping("/summary")
    public R<Map<String, Object>> summary(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query(MemberActions.ADMIN_ASYNC_SUMMARY,
                () -> adminAsyncService.summary(memberId));
    }

    /**
     * 分页列表（status/type 可选过滤）
     *
     * @param memberId 当前登录账号（JWT 注入，过管理员校验）
     * @param status   可选，按命令状态过滤
     * @param cmdType  可选，按命令类型过滤
     * @param page     页码（从 1 起，默认 1）
     * @param size     每页条数（默认 20）
     * @return 分页结果（records/total/page/size）
     */
    @GetMapping("/list")
    public R<Map<String, Object>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) String cmdType,
                                       @RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "20") long size) {
        return queryTemplate.query(MemberActions.ADMIN_ASYNC_LIST,
                () -> adminAsyncService.list(memberId, status, cmdType, page, size));
    }

    /**
     * 手动重试：DEAD/PENDING 重置为待执行（清零重试计数，立即执行）
     *
     * @param memberId 当前登录账号（JWT 注入，过管理员校验）
     * @param id       异步命令 id
     * @return 重试受理（无业务数据）
     */
    @PostMapping("/{id}/retry")
    public R<Void> retry(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId, @PathVariable Long id) {
        return writeTemplate.write(MemberActions.ADMIN_ASYNC_RETRY,
                () -> adminAsyncService.retry(memberId, id));
    }

    /**
     * 重占：卡单处理——把卡死的 RUNNING 强制放回队列（先确认执行器已死再操作）
     *
     * @param memberId 当前登录账号（JWT 注入，过管理员校验）
     * @param id       异步命令 id
     * @return 重占受理（无业务数据）
     */
    @PostMapping("/{id}/reclaim")
    public R<Void> reclaim(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId, @PathVariable Long id) {
        return writeTemplate.write(MemberActions.ADMIN_ASYNC_RECLAIM,
                () -> adminAsyncService.reclaim(memberId, id));
    }

    /**
     * 终止：卡单或待执行任务直接置死信（不再重试）
     *
     * @param memberId 当前登录账号（JWT 注入，过管理员校验）
     * @param id       异步命令 id
     * @return 终止受理（无业务数据）
     */
    @PostMapping("/{id}/kill")
    public R<Void> kill(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId, @PathVariable Long id) {
        return writeTemplate.write(MemberActions.ADMIN_ASYNC_KILL,
                () -> adminAsyncService.kill(memberId, id));
    }
}
