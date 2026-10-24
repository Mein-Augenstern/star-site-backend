package com.xp.starsite.family.dal.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭数据对象（family 表镜像，DAL 自持——不依赖任何上层模型）。
 * 领域语义见 core-model 的 entity/Family。
 *
 * @author muyi
 * @since 2026-10
 */
@Data
public class FamilyDO {

    /** 主键 */
    private Long id;

    /** 家庭名 */
    private String name;

    /** 顶梁柱账号 id（一账号一家庭） */
    private Long ownerMemberId;

    /** 备注 */
    private String remark;

    /** 逻辑删标志（0 正常 1 已删） */
    private Integer deleted;

    /** 创建时间（DB 维护） */
    private LocalDateTime createdAt;

    /** 更新时间（DB 维护） */
    private LocalDateTime updatedAt;
}
