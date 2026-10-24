package com.xp.starsite.finance.repository;

import com.xp.starsite.finance.entity.FinanceTransaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 记账流水仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author muyi
 * @since 2026-10
 */
public interface FinanceTransactionRepository {

    /** 保存（新增；雪花 id 应用侧先赋值；导入行靠 (source, external_id) 唯一键幂等，重复由 DuplicateKeyException 承载） */
    FinanceTransaction save(FinanceTransaction transaction);

    /** 按 id 取流水（含逻辑删过滤） */
    Optional<FinanceTransaction> findById(Long id);

    /** 逻辑删 */
    void markDeleted(Long id);

    /** 转账组两腿（成对回滚/对账用，含逻辑删过滤） */
    List<FinanceTransaction> findByTransferGroupId(Long transferGroupId);

    /** 流水分页：限定 owner（必填）+ 可选账户/方向 + 交易时间窗 [start, end)，按交易时间倒序 */
    List<FinanceTransaction> pageByOwner(Long ownerMemberId, Long accountId, Integer direction,
                                         LocalDateTime startTime, LocalDateTime endTime,
                                         long offset, int limit);

    /** 流水分页计数（与 pageByOwner 同条件） */
    long countByOwner(Long ownerMemberId, Long accountId, Integer direction,
                      LocalDateTime startTime, LocalDateTime endTime);

    /** 某方向流水（时间窗 [start, end)，end 可空=不设上界；category 可空=不限）：收支/现金流/分类占比/预算占用共用 */
    List<FinanceTransaction> listByDirectionAndPeriod(Long ownerMemberId, int direction,
                                                      LocalDateTime startTime, LocalDateTime endTime,
                                                      String category);

    /** 收+支两类流水（近 12 个月趋势图共用，时间窗 [start, end)） */
    List<FinanceTransaction> listIncomeExpensePeriod(Long ownerMemberId,
                                                     LocalDateTime startTime, LocalDateTime endTime);
}
