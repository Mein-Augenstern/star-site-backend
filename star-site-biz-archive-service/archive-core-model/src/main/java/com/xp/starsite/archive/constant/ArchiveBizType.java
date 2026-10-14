package com.xp.starsite.archive.constant;

/**
 * archive 域事件信封业务类型：MQ 信封第三参 bizType（标识业务对象类型，区别于提醒规则 bizType）。
 * 值随事件落库/消费路由，一经上线只能新增、不能改名。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class ArchiveBizType {

    /** 资产对象 */
    public static final String ASSET = "asset";

    /** 证件对象 */
    public static final String DOCUMENT = "document";

    private ArchiveBizType() {
    }
}
