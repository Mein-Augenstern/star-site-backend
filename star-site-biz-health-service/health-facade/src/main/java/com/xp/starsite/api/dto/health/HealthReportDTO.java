package com.xp.starsite.api.dto.health;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 体检报告快照（跨服务传输用）：报告元数据 + 指标全集。
 * 归属字段冗余（familyMemberId），消费方据此校验报告确属目标成员。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class HealthReportDTO implements Serializable {

    /** 主键（报告 id） */
    private Long id;

    /** 家庭成员 id（冗余，消费方据此校验报告归属） */
    private Long familyMemberId;

    /** 报告年份 */
    private Integer reportYear;

    /** 体检日期 */
    private LocalDate checkupDate;

    /** 医院 */
    private String hospital;

    /** 标题 */
    private String title;

    /** 总结 */
    private String summary;

    /** 报告文件 id（file 服务） */
    private Long fileId;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 详情接口返回，列表场景为 null */
    private List<HealthIndicatorDTO> indicators;

    /** 便捷方法：按指标名取首个指标（复查闭环按名对齐基线用） */
    public HealthIndicatorDTO indicatorByName(String itemName) {
        if (indicators == null || itemName == null) {
            return null;
        }
        return indicators.stream()
                .filter(i -> itemName.equals(i.getItemName()))
                .findFirst()
                .orElse(null);
    }

    /** 便捷方法：参考区间展示文案（如 "3.5-5.7"，无区间返回 ""；去 DECIMAL 小数尾零） */
    public static String refRangeText(BigDecimal refLow, BigDecimal refHigh) {
        if (refLow == null && refHigh == null) {
            return "";
        }
        if (refLow == null) {
            return "<=" + strip(refHigh);
        }
        if (refHigh == null) {
            return ">=" + strip(refLow);
        }
        return strip(refLow) + "-" + strip(refHigh);
    }

    private static String strip(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}
