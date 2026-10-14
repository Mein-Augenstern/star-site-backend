package com.xp.starsite.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 家庭成员快照（跨服务传输用：归属校验 + 展示冗余）
 *
 * @author xiaopu
 * @since 2026-10
 */
@Data
public class FamilyMemberSnapshotDTO implements Serializable {

    /** 成员档案 id */
    private Long id;

    /** 所属家庭 id */
    private Long familyId;

    /** 姓名/称呼 */
    private String name;

    /** self/spouse/father/mother/child/other */
    private String relation;

    /** 0未知 1男 2女 */
    private Integer gender;

    /** 生日 */
    private LocalDate birthDate;

    /** 联系电话 */
    private String phone;
}
