package com.xp.starsite.reminder.repository;

import com.xp.starsite.reminder.entity.Notification;

import java.util.List;
import java.util.Optional;

/**
 * 站内通知仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author muyi
 * @since 2026-10
 */
public interface NotificationRepository {

    /** 保存（新增；回填自增 id） */
    Notification save(Notification notification);

    /** 按 id 取通知 */
    Optional<Notification> findById(Long id);

    /** 未读数（铃铛角标） */
    long countUnread(Long ownerMemberId);

    /** 通知总数（分页 count 先行） */
    long countByOwner(Long ownerMemberId);

    /** 通知分页：归属账号，按 id 倒序 */
    List<Notification> pageByOwner(Long ownerMemberId, long offset, int limit);

    /** 标记已读（归属校验内嵌 WHERE，非本人通知 no-op） */
    void markRead(Long id, Long ownerMemberId);
}
