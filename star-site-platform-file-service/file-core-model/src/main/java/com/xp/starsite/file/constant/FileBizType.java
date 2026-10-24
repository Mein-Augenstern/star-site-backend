package com.xp.starsite.file.constant;

/**
 * file 域业务类型（file_record.biz_type 归档桶）。
 * 上传方经 /file/upload?bizType= 自由传入（report/bill_import/...），本类只收口服务端自有的兜底值。
 *
 * @author muyi
 * @since 2026-10
 */
public final class FileBizType {

    /** 未声明业务类型的归档桶 */
    public static final String OTHER = "other";

    private FileBizType() {
    }
}
