package com.xp.starsite.finance.service;

import com.xp.starsite.finance.dto.GoalSaveDTO;
import com.xp.starsite.finance.vo.GoalVO;

import java.util.List;

/**
 * 家庭目标服务。首参 memberId 用于归属校验。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface GoalService {

    GoalVO create(Long memberId, GoalSaveDTO dto);

    /** 目标列表（含进度） */
    List<GoalVO> list(Long memberId);

    /** 更新（名称/金额/进度/截止日） */
    GoalVO update(Long memberId, Long id, GoalSaveDTO dto);

    void delete(Long memberId, Long id);
}
