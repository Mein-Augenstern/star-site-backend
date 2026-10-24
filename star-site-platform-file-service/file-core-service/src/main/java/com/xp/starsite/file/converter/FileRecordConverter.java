package com.xp.starsite.file.converter;

import com.xp.starsite.file.dal.dataobject.FileRecordDO;
import com.xp.starsite.file.entity.FileRecord;
import org.springframework.beans.BeanUtils;

/**
 * 文件记录模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author muyi
 * @since 2026-10
 */
public final class FileRecordConverter {

    private FileRecordConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static FileRecordDO toDO(FileRecord record) {
        FileRecordDO row = new FileRecordDO();
        row.setId(record.getId());
        row.setFileName(record.getFileName());
        row.setStorageKey(record.getStorageKey());
        row.setContentType(record.getContentType());
        row.setSizeBytes(record.getSizeBytes());
        row.setBizType(record.getBizType());
        row.setUploaderMemberId(record.getUploaderMemberId());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static FileRecord toEntity(FileRecordDO row) {
        FileRecord record = new FileRecord();
        BeanUtils.copyProperties(row, record);
        return record;
    }
}
