package com.xp.starsite.insurance.constant;

/**
 * insurance 域事件信封业务类型：MQ 信封第三参 bizType（标识业务对象类型，区别于提醒规则 bizType）。
 * 值随事件落库/消费路由，一经上线只能新增、不能改名。
 *
 * @author muyi
 * @since 2026-10
 */
public final class InsuranceBizType {

    /** 保单对象 */
    public static final String POLICY = "policy";

    private InsuranceBizType() {
    }
}
