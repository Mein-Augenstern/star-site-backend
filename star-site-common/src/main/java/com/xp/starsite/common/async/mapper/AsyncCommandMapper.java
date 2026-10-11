package com.xp.starsite.common.async.mapper;

import com.xp.starsite.common.async.AsyncCommand;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 异步命令 mapper（MyBatis 官方注解式，无 MyBatis-Plus）。
 * 独占子包（common.async.mapper）：包即扫描边界，避免同包的 Executor 等接口
 * 被 @MapperScan 误注册成 mybatis 代理。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface AsyncCommandMapper {

    /** 新建命令（created_at/updated_at 由 DB 默认值维护） */
    @Insert("INSERT INTO async_command (cmd_type, biz_key, payload, status, max_retry, retry_count, next_run_at, owner) " +
            "VALUES (#{cmdType}, #{bizKey}, #{payload}, #{status}, #{maxRetry}, #{retryCount}, #{nextRunAt}, #{owner})")
    int insert(AsyncCommand cmd);

    /** 幂等提交检查：同 type+key 未完成命令数 */
    @Select("SELECT COUNT(*) FROM async_command " +
            "WHERE cmd_type = #{cmdType} AND biz_key = #{bizKey} AND status IN ('PENDING', 'RUNNING')")
    long countUnfinished(@Param("cmdType") String cmdType, @Param("bizKey") String bizKey);

    /** 按 id 集合取回（执行器回显用） */
    @Select("<script>SELECT * FROM async_command WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<AsyncCommand> selectByIds(@Param("ids") List<Long> ids);

    /** 按 id 更新执行状态（status/claim_token/retry_count/next_run_at/last_error） */
    @Update("UPDATE async_command SET status = #{status}, claim_token = #{claimToken}, " +
            "retry_count = #{retryCount}, next_run_at = #{nextRunAt}, last_error = #{lastError}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateState(AsyncCommand cmd);

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
