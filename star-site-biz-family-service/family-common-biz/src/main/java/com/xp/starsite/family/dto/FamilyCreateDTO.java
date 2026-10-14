package com.xp.starsite.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建家庭入参
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class FamilyCreateDTO {

    /** 家庭名（如 「我们仨」） */
    @NotBlank(message = "家庭名称不能为空")
    @Size(max = 64, message = "家庭名称最长 64 位")
    private String name;

    /** 备注 */
    @Size(max = 255, message = "备注最长 255 位")
    private String remark;
}
