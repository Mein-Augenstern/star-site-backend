package com.xp.starsite.health.constant;

/**
 * health 域提醒集成常量：事件 source 与提醒规则业务类型（bizType）。
 * <p>
 * source/bizType 会落 reminder 库并参与幂等键（source+bizType+bizId），
 * 值一经上线只能新增、不能改名（改名 = 幂等键漂移 = 重复规则）。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class HealthReminderBizType {

    /** 事件来源标识（幂等键第一段，与 nacos 服务名一致） */
    public static final String SOURCE = "star-site-biz-health";

    /** 产检里程碑提醒 */
    public static final String PRENATAL_CHECKUP = "prenatal_checkup";

    private HealthReminderBizType() {
    }
}
