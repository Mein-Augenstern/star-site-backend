package com.xp.starsite.archive.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭资产（archive_asset）：房产/车辆/存款理财台账。
 * 车辆以 next_inspect_date 为源发年检提醒（ANNUAL 重复）；current_value 汇入净资产曲线。
 * 纯 POJO——表结构映射在 DAL 的 mapper XML（resultType 驼峰自动映射）。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class ArchiveAsset {

    /** 雪花 id（应用侧赋值）：年检提醒事务消息需在 insert 前拿到 id 填进事件载荷 */
    private Long id;

    /** 家庭 id（多人协作预留冗余） */
    private Long familyId;

    /** 归属账号 id（member 服务） */
    private Long ownerMemberId;

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

    /** 当前估值 */
    private BigDecimal currentValue;

    /** 估值时间 */
    private LocalDateTime valuedAt;

    /** 下次年检日期（仅车辆，提醒源；非空即发 ANNUAL 提醒） */
    private LocalDate nextInspectDate;

    /** 备注 */
    private String remark;

    /** 逻辑删标志（0 正常 1 已删） */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
