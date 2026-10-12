package com.xp.starsite.reminder.mapper;

import com.xp.starsite.reminder.dal.dataobject.NotificationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 站内通知数据访问（DAL）：SQL 全部收敛在 mapper XML。
 * 投递历史不删，无 deleted 列。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface NotificationMapper {

    /** 新建通知（id DB 自增回填；created_at 由 DB 默认值维护） */
    int insert(NotificationDO notification);

    /** 按 id 取通知，不存在返回 null */
    NotificationDO selectById(@Param("id") Long id);

    /** 未读数（铃铛角标） */
    long countUnread(@Param("ownerMemberId") Long ownerMemberId);

    /** 通知总数（分页 count 先行） */
    long countByOwner(@Param("ownerMemberId") Long ownerMemberId);

    /** 通知分页：归属账号，按 id 倒序（count 先行后再取页） */
    List<NotificationDO> selectPageByOwner(@Param("ownerMemberId") Long ownerMemberId,
                                           @Param("offset") long offset,
                                           @Param("limit") int limit);

    /** 标记已读（归属校验内嵌 WHERE，非本人通知 no-op） */
    int markRead(@Param("id") Long id, @Param("ownerMemberId") Long ownerMemberId);
}
