package com.xp.starsite.reminder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知（前端铃铛数据源）
 */
@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long occurrenceId;

    private Long ownerMemberId;

    private String title;

    /** 0未读 1已读 */
    private Integer readFlag;

    private LocalDateTime createdAt;
}
