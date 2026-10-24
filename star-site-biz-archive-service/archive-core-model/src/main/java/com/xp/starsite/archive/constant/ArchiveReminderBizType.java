package com.xp.starsite.archive.constant;

/**
 * archive 域提醒集成常量：事件 source 与提醒规则业务类型（bizType）。
 * <p>
 * source/bizType 会落 reminder 库并参与幂等键（source+bizType+bizId），
 * 值一经上线只能新增、不能改名（改名 = 幂等键漂移 = 重复规则）。
 *
 * @author muyi
 * @since 2026-10
 */
public final class ArchiveReminderBizType {

    /** 事件来源标识（幂等键第一段；历史值，与 nacos 服务名 star-site-biz-archive 的漂移是有意保留） */
    public static final String SOURCE = "star-site-archive";

    /** 车辆年检提醒 */
    public static final String VEHICLE_INSPECT = "vehicle_inspect";

    /** 证件到期提醒 */
    public static final String DOCUMENT_EXPIRY = "document_expiry";

    private ArchiveReminderBizType() {
    }
}
