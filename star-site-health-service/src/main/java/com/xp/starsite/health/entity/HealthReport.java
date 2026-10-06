package com.xp.starsite.health.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体检报告
 */
@Data
@TableName("health_report")
public class HealthReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 家庭成员 id（family 服务的自然人档案） */
    private Long familyMemberId;

    private Integer reportYear;

    private LocalDate checkupDate;

    private String hospital;

    private String title;

    private String summary;

    /** 报告文件 id（file 服务） */
    private Long fileId;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
