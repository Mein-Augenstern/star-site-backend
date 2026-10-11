package com.xp.starsite.archive.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产视图
 */
@Data
public class AssetVO {

    private Long id;

    private String name;

    private String assetType;

    private Long holderMemberId;

    private String holderMemberName;

    private LocalDate acquireDate;

    private BigDecimal currentValue;

    private LocalDateTime valuedAt;

    private LocalDate nextInspectDate;

    private String remark;

    private LocalDateTime createdAt;
}
