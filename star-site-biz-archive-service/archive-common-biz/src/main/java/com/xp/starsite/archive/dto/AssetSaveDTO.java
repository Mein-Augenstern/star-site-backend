package com.xp.starsite.archive.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资产创建/编辑入参
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class AssetSaveDTO {

    @NotBlank(message = "资产名称不能为空")
    private String name;

    @NotBlank(message = "资产类型不能为空")
    private String assetType;

    /** 持有人，空 = 家庭共有 */
    private Long holderMemberId;

    private LocalDate acquireDate;

    @Min(value = 0, message = "估值不能为负")
    private BigDecimal currentValue;

    /** 下次年检日期（仅车辆） */
    private LocalDate nextInspectDate;

    private String remark;
}
