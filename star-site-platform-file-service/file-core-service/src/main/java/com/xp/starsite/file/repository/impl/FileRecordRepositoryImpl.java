package com.xp.starsite.file.repository.impl;

import com.xp.starsite.file.converter.FileRecordConverter;
import com.xp.starsite.file.dal.dataobject.FileRecordDO;
import com.xp.starsite.file.entity.FileRecord;
import com.xp.starsite.file.mapper.FileRecordMapper;
import com.xp.starsite.file.repository.FileRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 文件记录仓储实现：调用 dal 的 {@link FileRecordMapper}（DAO）操作数据库，
 * DO↔Entity 转换收敛于此——上层（service）只见领域模型。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Component
@RequiredArgsConstructor
public class FileRecordRepositoryImpl implements FileRecordRepository {

    private final FileRecordMapper fileRecordMapper;

    @Override
    public FileRecord save(FileRecord record) {
        FileRecordDO row = FileRecordConverter.toDO(record);
        fileRecordMapper.insert(row);
        record.setId(row.getId());
        return record;
    }

    @Override
    public Optional<FileRecord> findById(Long id) {
        return Optional.ofNullable(fileRecordMapper.selectById(id)).map(FileRecordConverter::toEntity);
    }
}
