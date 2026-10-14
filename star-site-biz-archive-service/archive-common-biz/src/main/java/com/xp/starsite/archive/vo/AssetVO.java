package com.xp.starsite.archive.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产视图
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class AssetVO {

    /** 资产 id */
    private Long id;

    /** 资产名称（如 工行储蓄卡/家庭唯一用车） */
    private String name;

    /** 资产类型：real_estate/vehicle/deposit/investment/fund/other */
    private String assetType;

    /** 持有人，null = 家庭共有 */
    private Long holderMemberId;

    /** 持有人姓名快照 */
    private String holderMemberName;

    /** 购置/开户日期 */
    private LocalDate acquireDate;

    /** 当前估值（元） */
    private BigDecimal currentValue;

    /** 估值时间 */
    private LocalDateTime valuedAt;

    /** 下次年检日期（仅车辆，年检提醒源） */
    private LocalDate nextInspectDate;

    /** 备注 */
    private String remark;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;
}
