package com.xp.starsite.member.converter;

import com.xp.starsite.common.async.AsyncCommand;
import com.xp.starsite.member.dal.dataobject.AsyncCommandDO;
import org.springframework.beans.BeanUtils;

/**
 * 异步命令模型转换：DO（DAL 表映射）↔ AsyncCommand（common star-async 组件模型）。
 * 转换只在本层（core-service）发生——dal 不感知组件模型，common 不感知表结构。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class AsyncCommandConverter {

    private AsyncCommandConverter() {
    }

    /** 组件模型 → 落库对象 */
    public static AsyncCommandDO toDO(AsyncCommand cmd) {
        AsyncCommandDO row = new AsyncCommandDO();
        row.setId(cmd.getId());
        row.setCmdType(cmd.getCmdType());
        row.setBizKey(cmd.getBizKey());
        row.setPayload(cmd.getPayload());
        row.setStatus(cmd.getStatus());
        row.setMaxRetry(cmd.getMaxRetry());
        row.setRetryCount(cmd.getRetryCount());
        row.setNextRunAt(cmd.getNextRunAt());
        row.setLastError(cmd.getLastError());
        row.setOwner(cmd.getOwner());
        row.setClaimToken(cmd.getClaimToken());
        return row;
    }

    /** 落库对象 → 组件模型 */
    public static AsyncCommand toEntity(AsyncCommandDO row) {
        AsyncCommand cmd = new AsyncCommand();
        BeanUtils.copyProperties(row, cmd);
        return cmd;
    }
}
