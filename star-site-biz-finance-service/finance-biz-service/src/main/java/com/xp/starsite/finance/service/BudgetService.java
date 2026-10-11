package com.xp.starsite.finance.service;

import com.xp.starsite.finance.dto.BudgetSaveDTO;
import com.xp.starsite.finance.vo.BudgetVO;

import java.util.List;

/**
 * 月度分类预算服务。首参 memberId 用于归属校验。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface BudgetService {

    /** 保存预算（同 owner+category+period upsert） */
    BudgetVO save(Long memberId, BudgetSaveDTO dto);

    /** 某月预算列表（含当月使用率，超支 remaining 为负） */
    List<BudgetVO> list(Long memberId, String period);

    /** 删除预算 */
    void delete(Long memberId, Long id);
}
