package com.xp.starsite.reminder.converter;

import com.xp.starsite.reminder.dal.dataobject.NotificationDO;
import com.xp.starsite.reminder.entity.Notification;
import org.springframework.beans.BeanUtils;

/**
 * 站内通知模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class NotificationConverter {

    private NotificationConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static NotificationDO toDO(Notification notification) {
        NotificationDO row = new NotificationDO();
        row.setId(notification.getId());
        row.setOccurrenceId(notification.getOccurrenceId());
        row.setOwnerMemberId(notification.getOwnerMemberId());
        row.setTitle(notification.getTitle());
        row.setReadFlag(notification.getReadFlag());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static Notification toEntity(NotificationDO row) {
        Notification notification = new Notification();
        BeanUtils.copyProperties(row, notification);
        return notification;
    }
}
