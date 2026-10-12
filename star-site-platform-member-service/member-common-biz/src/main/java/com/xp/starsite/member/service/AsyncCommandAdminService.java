package com.xp.starsite.member.service;

import java.util.Map;

/**
 * 异步命令管控服务（管理员运维：卡单/重试/监控）。
 * 管理员校验（ADMIN_MEMBER_IDS 白名单 + 账号角色核验）与管控查询/处置编排收敛于此，
 * Controller 只透传 memberId 与端点参数。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface AsyncCommandAdminService {

    /**
     * 监控总览：各状态计数 + 积压 + 近期命令按类型分布
     *
     * @param memberId 当前登录账号（过管理员校验）
     * @return 各状态计数（pending/running/success/dead）、积压数 backlogDue 与近 500 条按类型分布 byType
     * @throws com.xp.starsite.common.exception.BizException 无管理员权限（403）
     */
    Map<String, Object> summary(Long memberId);

    /**
     * 管控分页列表（status/type 可选过滤）
     *
     * @param memberId 当前登录账号（过管理员校验）
     * @param status   可选，按命令状态过滤
     * @param cmdType  可选，按命令类型过滤
     * @param page     页码（从 1 起）
     * @param size     每页条数
     * @return 分页结果（records/total/page/size）
     * @throws com.xp.starsite.common.exception.BizException 无管理员权限（403）
     */
    Map<String, Object> list(Long memberId, String status, String cmdType, long page, long size);

    /**
     * 手动重试：DEAD/PENDING 重置为待执行（清零重试计数，立即执行）
     *
     * @param memberId 当前登录账号（过管理员校验）
     * @param id       异步命令 id
     * @throws com.xp.starsite.common.exception.BizException 命令不存在（404）、
     *                                                       任务执行中需先重占（400）、已成功任务无需重试（400）、无管理员权限（403）
     */
    void retry(Long memberId, Long id);

    /**
     * 重占：卡单处理——把卡死的 RUNNING 强制放回队列（先确认执行器已死再操作）
     *
     * @param memberId 当前登录账号（过管理员校验）
     * @param id       异步命令 id
     * @throws com.xp.starsite.common.exception.BizException 命令不存在（404）、仅 RUNNING 状态可重占（400）、无管理员权限（403）
     */
    void reclaim(Long memberId, Long id);

    /**
     * 终止：卡单或待执行任务直接置死信（不再重试）
     *
     * @param memberId 当前登录账号（过管理员校验，进 last_error 审计）
     * @param id       异步命令 id
     * @throws com.xp.starsite.common.exception.BizException 命令不存在（404）、已成功任务不可终止（400）、无管理员权限（403）
     */
    void kill(Long memberId, Long id);
}
