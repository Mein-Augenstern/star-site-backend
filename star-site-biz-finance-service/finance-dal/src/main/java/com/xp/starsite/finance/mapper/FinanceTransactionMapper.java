package com.xp.starsite.finance.mapper;

import com.xp.starsite.finance.entity.FinanceTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记账流水数据访问（DAL）：finance_transaction，逻辑删。
 * 幂等依赖 uk_source_ext(source, external_id) 唯一键——重复导入由 DuplicateKeyException 语义承载。
 * 转账两笔流水共享 transferGroupId，同事务成对插入、成对删除。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Mapper
public interface FinanceTransactionMapper {

    /** 插入流水（雪花 id 由业务层 insert 前赋值；导入行靠 (source, external_id) 唯一键幂等） */
    int insert(FinanceTransaction transaction);

    /** 按 id 取流水（含逻辑删过滤，不存在返回 null） */
    FinanceTransaction selectById(@Param("id") Long id);

    /** 逻辑删：deleted 置 1 */
    int markDeleted(@Param("id") Long id);

    /** 转账组两腿（成对回滚/对账用，含逻辑删过滤） */
    List<FinanceTransaction> selectByTransferGroupId(@Param("transferGroupId") Long transferGroupId);

    /** 流水分页：限定 owner（必填）+ 可选账户/方向 + 交易时间窗 [start, end)，按交易时间倒序 */
    List<FinanceTransaction> selectPage(@Param("ownerMemberId") Long ownerMemberId,
                                        @Param("accountId") Long accountId,
                                        @Param("direction") Integer direction,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime,
                                        @Param("offset") long offset,
                                        @Param("limit") int limit);

    /** 流水分页计数（与 selectPage 同条件） */
    long countByCondition(@Param("ownerMemberId") Long ownerMemberId,
                          @Param("accountId") Long accountId,
                          @Param("direction") Integer direction,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);

    /** 某方向流水（时间窗 [start, end)，end 可空=不设上界；category 可空=不限）：收支/现金流/分类占比/预算占用共用 */
    List<FinanceTransaction> selectByDirectionAndPeriod(@Param("ownerMemberId") Long ownerMemberId,
                                                        @Param("direction") int direction,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime,
                                                        @Param("category") String category);

    /** 收+支两类流水（近 12 个月趋势图共用，时间窗 [start, end)） */
    List<FinanceTransaction> selectIncomeExpensePeriod(@Param("ownerMemberId") Long ownerMemberId,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);
}
