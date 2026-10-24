package com.xp.starsite.reminder.mapper;

import com.xp.starsite.reminder.dal.dataobject.ReminderRuleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 提醒规则数据访问（DAL）：SQL 全部收敛在 mapper XML，业务层只见业务语义方法。
 * 本表无 deleted 列——撤销走物理删，保证幂等键 (source,biz_type,biz_id) 唯一键干净。
 *
 * @author muyi
 * @since 2026-10
 */
@Mapper
public interface ReminderRuleMapper {

    /** 新建规则（id DB 自增回填；created_at/updated_at 由 DB 默认值维护） */
    int insert(ReminderRuleDO rule);

    /** 按幂等键取规则（事件消费端 upsert：source+bizType+bizId 唯一），不存在返回 null */
    ReminderRuleDO selectByIdempotentKey(@Param("source") String source,
                                         @Param("bizType") String bizType,
                                         @Param("bizId") Long bizId);

    /** 按 id 取规则，不存在返回 null */
    ReminderRuleDO selectById(@Param("id") Long id);

    /** 显式更新规则全量业务列（upsert 更新分支、自建规则回填 biz_id 用；updated_at 由 DB 维护） */
    int update(ReminderRuleDO rule);

    /** 撤销删除（物理删）：衍生实例/通知的清理由调用方另行处理 */
    int deleteById(@Param("id") Long id);

    /** 每日扫描：全部启用中的规则 */
    List<ReminderRuleDO> selectEnabled();

    /** 用户自建规则列表：指定来源、归属成员集合内、启用中，按 id 倒序 */
    List<ReminderRuleDO> selectActiveBySourceAndMembers(@Param("source") String source,
                                                        @Param("familyMemberIds") Collection<Long> familyMemberIds);
}
