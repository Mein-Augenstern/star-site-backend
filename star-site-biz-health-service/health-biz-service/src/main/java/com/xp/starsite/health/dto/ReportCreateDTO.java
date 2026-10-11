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

    @NotNull(message = "家庭成员不能为空")
    private Long familyMemberId;

    @NotNull(message = "报告年份不能为空")
    @Min(value = 1990, message = "年份不合法")
    @Max(value = 2100, message = "年份不合法")
    private Integer reportYear;

    private LocalDate checkupDate;

    @Size(max = 128, message = "医院名最长 128 位")
    private String hospital;

    @Size(max = 128, message = "标题最长 128 位")
    private String title;

    @Size(max = 1000, message = "总结最长 1000 位")
    private String summary;

    /** 报告文件 id（先调 file 服务上传） */
    private Long fileId;

    @Size(max = 255, message = "备注最长 255 位")
    private String remark;
}
