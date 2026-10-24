package com.xp.starsite.api.event.reminder;

import lombok.Data;

import java.io.Serializable;

/**
 * 提醒事件基类：幂等键三件套（source + bizType + bizId）。
 * reminder-service 以此定位唯一规则，Register/Update 均为 upsert，Cancel 为删除。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public abstract class ReminderEvent implements Serializable {

    /** 发出方服务名（冗余，消费端不再依赖信封） */
    private String source;

    /** 业务类型，如 policy_payment / policy_expiry / report_checkup */
    private String bizType;

    /** 业务主键 */
    private Long bizId;
}
