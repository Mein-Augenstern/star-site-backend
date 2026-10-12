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

    /**
     * 创建证件（有效期非空 → 发到期提醒事件）
     *
     * @param memberId 当前登录账号
     * @param dto      证件内容（类型/名称/持有人/有效期等）
     * @return 新建证件视图
     * @throws com.xp.starsite.common.exception.BizException 持有人不属于本家庭（403）、
     *                                                       不支持的证件类型（400）
     */
    DocumentVO create(Long memberId, DocumentSaveDTO dto);

    /**
     * 证件分页（筛选：持有人、类型）
     *
     * @param memberId        当前登录账号
     * @param holderMemberId  可选，按持有人过滤（须属于本家庭）
     * @param docType         可选，按证件类型过滤
     * @param page            页码，从 1 起
     * @param size            每页条数
     * @return 证件分页
     * @throws com.xp.starsite.common.exception.BizException 指定持有人不属于本家庭（403）
     */
    PageVO<DocumentVO> list(Long memberId, Long holderMemberId, String docType, long page, long size);

    /**
     * 详情
     *
     * @param memberId 当前登录账号
     * @param id       证件 id
     * @return 证件视图
     * @throws com.xp.starsite.common.exception.BizException 证件不存在（400）或非本人证件（403）
     */
    DocumentVO getDetail(Long memberId, Long id);

    /**
     * 更新（有效期变更重发/撤销提醒）
     *
     * @param memberId 当前登录账号
     * @param id       证件 id
     * @param dto      证件内容
     * @return 更新后的证件视图
     * @throws com.xp.starsite.common.exception.BizException 证件不存在（400）、非本人证件（403）、
     *                                                       持有人不属于本家庭（403）、不支持的证件类型（400）
     */
    DocumentVO update(Long memberId, Long id, DocumentSaveDTO dto);

    /**
     * 删除（撤销到期提醒）
     *
     * @param memberId 当前登录账号
     * @param id       证件 id
     * @throws com.xp.starsite.common.exception.BizException 证件不存在（400）或非本人证件（403）
     */
    void delete(Long memberId, Long id);
}
