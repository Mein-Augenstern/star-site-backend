package com.xp.starsite.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 收支分类（V1 内置枚举 + 预留表化，F4 开放自定义）
 *
 * @author muyi
 * @since 2026-10
 */
@Getter
@AllArgsConstructor
public enum FinanceCategories {

    // 支出类
    FOOD("餐饮"), HOUSE("住房"), MORTGAGE("房贷"), CAR("用车"),
    INSURANCE("保费"), MEDICAL("医疗"), EDUCATION("教育"),
    SHOPPING("购物"), PARENT("孝亲"), CHILD("育儿"), OTHER_EXPENSE("其他支出"),
    // 收入类
    SALARY("工资"), BONUS("奖金"), INTEREST("利息"), RED_PACKET("红包"), OTHER_INCOME("其他收入");

    private final String label;

    public static boolean isValid(String name) {
        return Arrays.stream(values()).anyMatch(c -> c.name().equals(name));
    }

    public static String labelOf(String name) {
        return Arrays.stream(values())
                .filter(c -> c.name().equals(name))
                .findFirst().map(FinanceCategories::getLabel).orElse(name);
    }
}
