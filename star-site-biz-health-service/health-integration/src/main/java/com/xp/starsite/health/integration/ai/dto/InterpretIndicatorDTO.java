package com.xp.starsite.health.integration.ai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 解读请求的单条指标行（跨语言契约）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class InterpretIndicatorDTO implements Serializable {

    /** 指标名 */
    private String name;

    /** 结果（数值或文本，已渲染） */
    private String value;

    /** 单位，可空 */
    private String unit;

    /** 参考下限（字符串透传，展示原样），可空 */
    private String refLow;

    /** 参考上限，可空 */
    private String refHigh;

    /** 0正常 1偏高 2偏低 */
    private Integer abnormalFlag;
}
