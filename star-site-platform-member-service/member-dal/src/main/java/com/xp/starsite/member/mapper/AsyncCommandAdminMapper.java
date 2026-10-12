package com.xp.starsite.member.mapper;

import com.xp.starsite.common.async.AsyncCommand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异步命令管控查询（member 侧 DAL，供 AdminAsyncController 运维端使用）。
 * 表 async_command 归 star-async（common）组件所有，common 侧 mapper 只含执行链路方法；
 * 管控端（计数/积压/分页/手动处置）SQL 收敛在本 mapper XML，不改 common。
 * 无逻辑删（表无 deleted 列）；状态机语义与 common AsyncCommandMapper.updateState 对齐。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface AsyncCommandAdminMapper {

    /** 各状态计数（监控总览） */
    long countByStatus(@Param("status") String status);

    /** 积压：到期未执行的 PENDING 数 */
    long countBacklogDue(@Param("now") LocalDateTime now);

    /** 近 N 条（按 id 倒序，运维肉眼判断类型分布） */
    List<AsyncCommand> selectRecent(@Param("limit") int limit);

    /** 管控分页计数（status/cmdType 可选过滤） */
    long countPage(@Param("status") String status, @Param("cmdType") String cmdType);

    /** 管控分页（status/cmdType 可选过滤，id 倒序 + LIMIT OFFSET） */
    List<AsyncCommand> selectPage(@Param("status") String status,
                                  @Param("cmdType") String cmdType,
                                  @Param("offset") long offset,
                                  @Param("limit") long limit);

    /** 按 id 取命令（不存在返回 null） */
    AsyncCommand selectById(@Param("id") Long id);

    /**
     * 手动处置回写（重试/重占/终止）：status/claim_token/retry_count/next_run_at/last_error，
     * 列集与 common AsyncCommandMapper.updateState 一致（updated_at 由 DB ON UPDATE 维护，显式 NOW() 对齐）。
     */
    int updateState(AsyncCommand cmd);
}
