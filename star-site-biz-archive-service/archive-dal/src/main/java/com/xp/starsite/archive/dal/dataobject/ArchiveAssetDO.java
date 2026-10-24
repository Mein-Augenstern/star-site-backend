package com.xp.starsite.archive.dal.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭资产数据对象（archive_asset 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/ArchiveAsset。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class ArchiveAssetDO {

    /** 主键（雪花 id，应用侧赋值） */
    private Long id;

    /** 家庭 id */
    private Long familyId;

    /** 归属账号 id */
    private Long ownerMemberId;

    /** 资产名称 */
    private String name;

    /** 资产类型（real_estate/vehicle/deposit/...） */
    private String assetType;

    /** 持有家庭成员 id（家庭共有可空） */
    private Long holderMemberId;

    /** 持有人姓名快照 */
    private String holderMemberName;

    /** 购置日期 */
    private LocalDate acquireDate;

    /** 当前估值 */
    private BigDecimal currentValue;

    /** 估值时点 */
    private LocalDateTime valuedAt;

    /** 下次年检日（车辆类） */
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
