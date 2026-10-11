package com.xp.starsite.archive.vo;

import lombok.Data;

import java.util.List;

/**
 * 简单分页视图（域内自持）
 */
@Data
public class PageVO<T> {

    private List<T> records;

    private long total;

    private long page;

    private long size;

    public static <T> PageVO<T> of(List<T> records, long total, long page, long size) {
        PageVO<T> vo = new PageVO<>();
        vo.setRecords(records);
        vo.setTotal(total);
        vo.setPage(page);
        vo.setSize(size);
        return vo;
    }
}
