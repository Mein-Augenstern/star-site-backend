package com.xp.starsite.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 家庭成员快照（跨服务传输用：归属校验 + 展示冗余）
 */
@Data
public class FamilyMemberSnapshotDTO implements Serializable {

    private Long id;

    private Long familyId;

    private String name;

    /** self/spouse/father/mother/child/other */
    private String relation;

    /** 0未知 1男 2女 */
    private Integer gender;

    private LocalDate birthDate;

    private String phone;
}
