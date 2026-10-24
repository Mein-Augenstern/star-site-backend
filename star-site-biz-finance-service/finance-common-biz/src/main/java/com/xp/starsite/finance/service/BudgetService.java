package com.xp.starsite.finance.service;

import com.xp.starsite.finance.dto.BudgetSaveDTO;
import com.xp.starsite.finance.vo.BudgetVO;

import java.util.List;

/**
 * 月度分类预算服务。首参 memberId 用于归属校验。
 *
 * @author muyi
 * @since 2026-10
 */
public interface BudgetService {

    /**
     * 保存预算（同 owner+category+period upsert）
     *
     * @param memberId 当前登录账号
     * @param dto      预算内容（分类/期间/金额）
     * @return 保存后的预算视图（含使用率）
     * @throws com.xp.starsite.common.exception.BizException 预算金额必须大于 0（400）、不支持的分类（400）
     */
    BudgetVO save(Long memberId, BudgetSaveDTO dto);

    /**
     * 某月预算列表（含当月使用率，超支 remaining 为负）
     *
     * @param memberId 当前登录账号
     * @param period   预算期间（yyyy-MM）
     * @return 预算列表
     */
    List<BudgetVO> list(Long memberId, String period);

    /**
     * 删除预算
     *
     * @param memberId 当前登录账号
     * @param id       预算 id
     * @throws com.xp.starsite.common.exception.BizException 预算不存在或非本人预算（400/403）
     */
    void delete(Long memberId, Long id);
}
