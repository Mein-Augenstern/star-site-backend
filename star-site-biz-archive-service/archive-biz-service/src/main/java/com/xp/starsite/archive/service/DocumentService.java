package com.xp.starsite.archive.service;

import com.xp.starsite.archive.dto.DocumentSaveDTO;
import com.xp.starsite.archive.vo.DocumentVO;
import com.xp.starsite.archive.vo.PageVO;

/**
 * 证件夹服务。首参 memberId 为当前登录顶梁柱，用于归属校验。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface DocumentService {

    /** 创建证件（有效期非空 → 发到期提醒事件） */
    DocumentVO create(Long memberId, DocumentSaveDTO dto);

    /** 证件分页（筛选：持有人、类型） */
    PageVO<DocumentVO> list(Long memberId, Long holderMemberId, String docType, long page, long size);

    /** 详情 */
    DocumentVO getDetail(Long memberId, Long id);

    /** 更新（有效期变更重发/撤销提醒） */
    DocumentVO update(Long memberId, Long id, DocumentSaveDTO dto);

    /** 删除（撤销到期提醒） */
    void delete(Long memberId, Long id);
}
