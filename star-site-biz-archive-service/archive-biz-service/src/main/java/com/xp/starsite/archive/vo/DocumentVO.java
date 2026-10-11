package com.xp.starsite.archive.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 证件视图
 */
@Data
public class DocumentVO {

    private Long id;

    private Long holderMemberId;

    private String holderMemberName;

    private String docType;

    private String docNo;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    private Long fileId;

    private String remark;

    private LocalDateTime createdAt;
}
