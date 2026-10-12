package com.xp.starsite.reminder.repository.impl;

import com.xp.starsite.reminder.converter.NotificationConverter;
import com.xp.starsite.reminder.dal.dataobject.NotificationDO;
import com.xp.starsite.reminder.entity.Notification;
import com.xp.starsite.reminder.mapper.NotificationMapper;
import com.xp.starsite.reminder.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 站内通知仓储实现：调用 dal 的 {@link NotificationMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationMapper notificationMapper;

    @Override
    public Notification save(Notification notification) {
        NotificationDO row = NotificationConverter.toDO(notification);
        notificationMapper.insert(row);
        notification.setId(row.getId());
        return notification;
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return Optional.ofNullable(notificationMapper.selectById(id)).map(NotificationConverter::toEntity);
    }

    @Override
    public long countUnread(Long ownerMemberId) {
        return notificationMapper.countUnread(ownerMemberId);
    }

    @Override
    public long countByOwner(Long ownerMemberId) {
        return notificationMapper.countByOwner(ownerMemberId);
    }

    @Override
    public List<Notification> pageByOwner(Long ownerMemberId, long offset, int limit) {
        return notificationMapper.selectPageByOwner(ownerMemberId, offset, limit)
                .stream().map(NotificationConverter::toEntity).toList();
    }

    @Override
    public void markRead(Long id, Long ownerMemberId) {
        notificationMapper.markRead(id, ownerMemberId);
    }
}
