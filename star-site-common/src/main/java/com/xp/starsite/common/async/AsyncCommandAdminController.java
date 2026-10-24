package com.xp.starsite.common.async;

import com.xp.starsite.common.constants.AuthConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * star-async 异步命令管控端点（管理员运维：卡单/重试/监控）。
 * <p>
 * 2026-10-15 自 member 收敛入组件：随 star.async.enabled 开关条件装配
 * （@ConditionalOnProperty——未启用服务组件扫描自动跳过，零感知）；基路径经构造注入（star.async.admin-base-path 可配，
 * 每服务独立 DB 后各服务管各自的 async_command 表，网关按路径前缀路由到对应服务）。
 * 鉴权：JWT 必过网关；管理员白名单校验在 {@link AsyncCommandAdminService} 内完成，
 * Controller 只透传 memberId 与端点参数（军规：控制器零业务逻辑）。
 * 业务逻辑统一经模板包裹（动作命名 async.admin.*；2026-10-15 前历史动作为
 * member.admin.async.*，旧值仅存于历史日志不再使用）。
 *
 * @author muyi
 * @since 2026-10
 */
@RestController
@RequestMapping("${star.async.admin-base-path:/api/admin/async}")
@ConditionalOnProperty(name = "star.async.enabled", havingValue = "true")
@RequiredArgsConstructor
public class AsyncCommandAdminController {

    private final AsyncCommandAdminService adminService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 监控总览：各状态计数 + 积压 + 近期死信
     *
     * @param memberId 当前登录账号（JWT 注入，过管理员白名单校验）
     * @return 各状态计数、积压数与近 500 条按类型分布
     */
    @GetMapping("/summary")
    public R<Map<String, Object>> summary(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query(AsyncAdminActions.SUMMARY,
                () -> adminService.summary(memberId));
    }

    /**
     * 分页列表（status/type 可选过滤）
     *
     * @param memberId 当前登录账号（JWT 注入，过管理员白名单校验）
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
        return queryTemplate.query(AsyncAdminActions.LIST,
                () -> adminService.list(memberId, status, cmdType, page, size));
    }

    /**
     * 手动重试：DEAD/PENDING 重置为待执行（清零重试计数，立即执行）
     *
     * @param memberId 当前登录账号（JWT 注入，过管理员白名单校验）
     * @param id       异步命令 id
     * @return 重试受理（无业务数据）
     */
    @PostMapping("/{id}/retry")
    public R<Void> retry(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                         @PathVariable("id") Long id) {
        return writeTemplate.write(AsyncAdminActions.RETRY,
                () -> adminService.retry(memberId, id));
    }

    /**
     * 重占：卡单处理——把卡死的 RUNNING 强制放回队列（先确认执行器已死再操作）
     *
     * @param memberId 当前登录账号（JWT 注入，过管理员白名单校验）
     * @param id       异步命令 id
     * @return 重占受理（无业务数据）
     */
    @PostMapping("/{id}/reclaim")
    public R<Void> reclaim(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                           @PathVariable("id") Long id) {
        return writeTemplate.write(AsyncAdminActions.RECLAIM,
                () -> adminService.reclaim(memberId, id));
    }

    /**
     * 终止：卡单或待执行任务直接置死信（不再重试）
     *
     * @param memberId 当前登录账号（JWT 注入，过管理员白名单校验）
     * @param id       异步命令 id
     * @return 终止受理（无业务数据）
     */
    @PostMapping("/{id}/kill")
    public R<Void> kill(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                        @PathVariable("id") Long id) {
        return writeTemplate.write(AsyncAdminActions.KILL,
                () -> adminService.kill(memberId, id));
    }

    /** 动作名常量（规范 async.admin.*，模板留痕用；2026-10-15 前历史动作为 member.admin.async.*） */
    static final class AsyncAdminActions {
        static final String SUMMARY = "async.admin.summary";
        static final String LIST = "async.admin.list";
        static final String RETRY = "async.admin.retry";
        static final String RECLAIM = "async.admin.reclaim";
        static final String KILL = "async.admin.kill";

        private AsyncAdminActions() {
        }
    }
}
