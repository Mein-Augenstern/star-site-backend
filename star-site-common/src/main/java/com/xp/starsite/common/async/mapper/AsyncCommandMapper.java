package com.xp.starsite.common.async.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xp.starsite.common.async.AsyncCommand;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 异步命令 mapper。独占子包（common.async.mapper）：包即扫描边界，
 * 避免同包的 Executor 等接口被 @MapperScan 误注册成 mybatis 代理。
 */
public interface AsyncCommandMapper extends BaseMapper<AsyncCommand> {

    /**
     * 乐观认领（两步令牌法，MySQL 无 UPDATE..RETURNING 的经典解）：
     * 先给到期 PENDING 打上本实例唯一 claim_token 置 RUNNING（并发实例互斥，谁写入算谁），
     * 再按令牌取回本轮认领的命令。
     */
    @Update("UPDATE async_command SET status = 'RUNNING', claim_token = #{token}, updated_at = NOW() " +
            "WHERE status = 'PENDING' AND next_run_at <= NOW() " +
            "AND id IN (SELECT id FROM (SELECT id FROM async_command " +
            "WHERE status = 'PENDING' AND next_run_at <= NOW() ORDER BY id LIMIT #{limit}) t)")
    int claim(@Param("token") String token, @Param("limit") int limit);

    @Select("SELECT * FROM async_command WHERE claim_token = #{token} ORDER BY id")
    List<AsyncCommand> findByToken(@Param("token") String token);

    /** 崩溃回收：RUNNING 超时（执行器进程死亡残留）回 PENDING 并清令牌 */
    @Update("UPDATE async_command SET status = 'PENDING', claim_token = NULL, updated_at = NOW() " +
            "WHERE status = 'RUNNING' AND updated_at < NOW() - INTERVAL #{timeoutMinutes} MINUTE")
    int reclaimStale(@Param("timeoutMinutes") int timeoutMinutes);
}
