package com.xp.starsite.common.async;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 异步命令（star-async 组件核心模型）
 *
 * 落 async_command 表的一行即一条待执行命令：状态机 PENDING/RUNNING/SUCCESS/DEAD，
 * 由 AsyncCommandConfig 的调度器认领执行，业务方经 AsyncCommandService 提交。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
@TableName("async_command")
public class AsyncCommand {

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCESS = "SUCCESS";
    public static final String DEAD = "DEAD";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 命令类型 = 执行器路由键 */
    private String cmdType;

    private String bizKey;

    /** 载荷 JSON */
    private String payload;

    private String status;

    private Integer maxRetry;

    private Integer retryCount;

    /** 到期时间（延迟队列锚点） */
    private LocalDateTime nextRunAt;

    private String lastError;

    /** 提交方服务名 */
    private String owner;

    /** 调度认领令牌（认领瞬间生成，处理完成后清空） */
    private String claimToken;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
