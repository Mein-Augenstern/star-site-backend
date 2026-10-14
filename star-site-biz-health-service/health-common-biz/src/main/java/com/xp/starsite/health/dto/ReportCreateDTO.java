package com.xp.starsite.health.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 上传报告入参
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class ReportCreateDTO {

    /** 家庭成员 id */
    @NotNull(message = "家庭成员不能为空")
    private Long familyMemberId;

    /** 报告年份 */
    @NotNull(message = "报告年份不能为空")
    @Min(value = 1990, message = "年份不合法")
    @Max(value = 2100, message = "年份不合法")
    private Integer reportYear;

    /** 体检日期 */
    private LocalDate checkupDate;

    /** 医院 */
    @Size(max = 128, message = "医院名最长 128 位")
    private String hospital;

    /** 标题 */
    @Size(max = 128, message = "标题最长 128 位")
    private String title;

    /** 总结 */
    @Size(max = 1000, message = "总结最长 1000 位")
    private String summary;

    /** 报告文件 id（先调 file 服务上传） */
    private Long fileId;

    /** 备注 */
    @Size(max = 255, message = "备注最长 255 位")
    private String remark;
}
