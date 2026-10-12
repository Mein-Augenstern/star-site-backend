package com.xp.starsite.finance.repository;

import com.xp.starsite.finance.entity.FinanceNetWorthSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 净资产快照仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 * 快照按 uk(owner, snap_date) 当日覆盖更新、从不删除——曲线为只增不减的时间序列。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface FinanceNetWorthSnapshotRepository {

    /** 落当日快照（新增；id 由 DB 自增、useGeneratedKeys 回填；并发插入由 DuplicateKeyException 兜底） */
    FinanceNetWorthSnapshot save(FinanceNetWorthSnapshot snapshot);

    /** 某账号某日快照（幂等判存用） */
    Optional<FinanceNetWorthSnapshot> findByOwnerAndDate(Long ownerMemberId, LocalDate snapDate);

    /** 覆盖当日快照三项合计 */
    void update(FinanceNetWorthSnapshot snapshot);

    /** 某账号净资产曲线（按快照日期升序） */
    List<FinanceNetWorthSnapshot> listByOwner(Long ownerMemberId);
}
