package com.xp.starsite.member.repository;

import com.xp.starsite.common.async.AsyncCommand;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 异步命令管控仓储（领域导向）：对上层（service）暴露 common 的 AsyncCommand 模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 * 表 async_command 归 star-async（common）组件所有，本仓储只服务 member 管控端。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface AsyncCommandAdminRepository {

    /** 各状态计数（监控总览） */
    long countByStatus(String status);

    /** 积压：到期未执行的 PENDING 数 */
    long countBacklogDue(LocalDateTime now);

    /** 近 N 条（按 id 倒序，运维肉眼判断类型分布） */
    List<AsyncCommand> recent(int limit);

    /** 管控分页计数（status/cmdType 可选过滤） */
    long countPage(String status, String cmdType);

    /** 管控分页（status/cmdType 可选过滤，id 倒序） */
    List<AsyncCommand> page(String status, String cmdType, long offset, long limit);

    /** 按 id 取命令 */
    Optional<AsyncCommand> findById(Long id);

    /** 手动处置回写（重试/重占/终止） */
    void updateState(AsyncCommand cmd);
}
