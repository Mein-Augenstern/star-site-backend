package com.xp.starsite.finance.mapper;

import com.xp.starsite.finance.dal.dataobject.FinanceNetWorthSnapshotDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 净资产快照数据访问（DAL）：finance_net_worth_snapshot，自增主键，无逻辑删。
 * uk(owner_member_id, snap_date) 幂等：当日已有则 update 覆盖，并发插入由 DuplicateKeyException 兜底。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface FinanceNetWorthSnapshotMapper {

    /** 插入快照（id 由 DB 自增，useGeneratedKeys 回填） */
    int insert(FinanceNetWorthSnapshotDO snapshot);

    /** 某账号某日快照（幂等判存用，XML LIMIT 1） */
    FinanceNetWorthSnapshotDO selectByOwnerAndDate(@Param("ownerMemberId") Long ownerMemberId,
                                                 @Param("snapDate") LocalDate snapDate);

    /** 更新快照金额（按 id，覆盖当日三项合计） */
    int update(FinanceNetWorthSnapshotDO snapshot);

    /** 某账号净资产曲线（按快照日期升序） */
    List<FinanceNetWorthSnapshotDO> selectByOwner(@Param("ownerMemberId") Long ownerMemberId);
}
