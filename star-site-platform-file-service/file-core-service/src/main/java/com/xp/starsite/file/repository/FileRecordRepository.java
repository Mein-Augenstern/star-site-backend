package com.xp.starsite.file.repository;

import com.xp.starsite.file.entity.FileRecord;

import java.util.Optional;

/**
 * 文件记录仓储（领域导向）：对上层（service）暴露领域模型，
 * 屏蔽 DO 与 SQL 细节——实现位于 core-service 模块内，底层调用 dal 的 DAO。
 *
 * @author muyi
 * @since 2026-10
 */
public interface FileRecordRepository {

    /** 保存（新增；回填自增 id） */
    FileRecord save(FileRecord record);

    /** 按 id 取（不存在返回 empty） */
    Optional<FileRecord> findById(Long id);
}
