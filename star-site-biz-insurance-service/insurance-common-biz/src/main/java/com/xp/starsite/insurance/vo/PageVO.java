package com.xp.starsite.insurance.vo;

import lombok.Data;

import java.util.List;

/**
 * 简单分页视图（域内自持，不跨域复用其他服务的 VO）
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class PageVO<T> {

    /** 当前页数据列表 */
    private List<T> records;

    /** 总条数 */
    private long total;

    /** 页码（1 起） */
    private long page;

    /** 页大小 */
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
