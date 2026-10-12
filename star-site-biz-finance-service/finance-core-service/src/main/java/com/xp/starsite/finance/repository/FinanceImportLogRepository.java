package com.xp.starsite.finance.repository;

import com.xp.starsite.finance.entity.FinanceImportLog;

import java.util.List;

/**
 * 账单导入记录仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 * 导入记录只追加、不删改（消费结果审计留痕，前端按记录轮询导入进度）。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface FinanceImportLogRepository {

    /** 追加一条导入记录（id 由 DB 自增、useGeneratedKeys 回填） */
    FinanceImportLog save(FinanceImportLog importLog);

    /** 导入记录列表：限定 owner + 可选文件，按 id 倒序（前端轮询导入进度） */
    List<FinanceImportLog> listByOwner(Long ownerMemberId, Long fileId);
}
