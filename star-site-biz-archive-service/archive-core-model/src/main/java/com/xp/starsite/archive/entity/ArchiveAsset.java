package com.xp.starsite.archive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭资产（房产/车辆/存款理财台账）
 */
@Data
@TableName("archive_asset")
public class ArchiveAsset {

    /** 雪花 id（应用侧赋值）：年检提醒事务消息需在 insert 前拿到 id 填进事件载荷 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long familyId;

    private Long ownerMemberId;

    private String name;

    /** real_estate/vehicle/deposit/investment/fund/other */
    private String assetType;

    /** 持有人，null = 家庭共有 */
    private Long holderMemberId;

    /** 持有人姓名快照 */
    private String holderMemberName;

    private LocalDate acquireDate;

    /** 当前估值 */
    private BigDecimal currentValue;

    /** 估值时间 */
    private LocalDateTime valuedAt;

    /** 下次年检日期（仅车辆，提醒源；非空即发 ANNUAL 提醒） */
    private LocalDate nextInspectDate;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
