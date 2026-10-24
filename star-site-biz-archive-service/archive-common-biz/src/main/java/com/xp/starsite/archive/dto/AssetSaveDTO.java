package com.xp.starsite.archive.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资产创建/编辑入参
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class AssetSaveDTO {

    /** 资产名称（如 工行储蓄卡/家庭唯一用车） */
    @NotBlank(message = "资产名称不能为空")
    private String name;

    /** 资产类型：real_estate/vehicle/deposit/investment/fund/other */
    @NotBlank(message = "资产类型不能为空")
    private String assetType;

    /** 持有人，空 = 家庭共有 */
    private Long holderMemberId;

    /** 购置/开户日期 */
    private LocalDate acquireDate;

    /** 当前估值（元） */
    @Min(value = 0, message = "估值不能为负")
    private BigDecimal currentValue;

    /** 下次年检日期（仅车辆） */
    private LocalDate nextInspectDate;

    /** 备注 */
    private String remark;
}
