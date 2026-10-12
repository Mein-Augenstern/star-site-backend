package com.xp.starsite.member.dal.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异步命令数据对象（async_command 表镜像，DAL 自持——不依赖任何上层模型）。
 * 表归 star-async（common）组件所有；本 DO 仅服务 member 管控端查询/处置，
 * 领域语义见 common 的 AsyncCommand。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class AsyncCommandDO {

    /** 主键 */
    private Long id;

    /** 命令类型（如 SEND_EMAIL） */
    private String cmdType;

    /** 业务幂等键 */
    private String bizKey;

    /** 命令载荷（JSON） */
    private String payload;

    /** 状态（PENDING/CLAIMED/DONE/FAILED/DEAD） */
    private String status;

    /** 最大重试次数 */
    private Integer maxRetry;

    /** 已重试次数 */
    private Integer retryCount;

    /** 下次可执行时间 */
    private LocalDateTime nextRunAt;

    /** 最近一次错误 */
    private String lastError;

    /** 认领者（实例标识） */
    private String owner;

    /** 认领令牌（重占/终止防串指令） */
    private String claimToken;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB ON UPDATE 维护） */
    private LocalDateTime updatedAt;
}
