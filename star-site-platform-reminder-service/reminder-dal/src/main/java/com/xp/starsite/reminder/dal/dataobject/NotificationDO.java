package com.xp.starsite.reminder.dal.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知数据对象（notification 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/Notification。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class NotificationDO {

    /** 主键 */
    private Long id;

    /** 所属提醒实例 id */
    private Long occurrenceId;

    /** 归属账号 id */
    private Long ownerMemberId;

    /** 通知标题 */
    private String title;

    /** 已读标志（0 未读 1 已读） */
    private Integer readFlag;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
