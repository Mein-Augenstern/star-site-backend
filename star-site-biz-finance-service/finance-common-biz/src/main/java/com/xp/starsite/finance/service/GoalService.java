package com.xp.starsite.finance.service;

import com.xp.starsite.finance.dto.GoalSaveDTO;
import com.xp.starsite.finance.vo.GoalVO;

import java.util.List;

/**
 * 家庭目标服务。首参 memberId 用于归属校验。
 *
 * @author muyi
 * @since 2026-10
 */
public interface GoalService {

    /**
     * 创建目标
     *
     * @param memberId 当前登录账号
     * @param dto      目标内容（名称/目标金额/截止日）
     * @return 新建目标视图（含进度）
     */
    GoalVO create(Long memberId, GoalSaveDTO dto);

    /**
     * 目标列表（含进度）
     *
     * @param memberId 当前登录账号
     * @return 目标列表
     */
    List<GoalVO> list(Long memberId);

    /**
     * 更新（名称/金额/进度/截止日）
     *
     * @param memberId 当前登录账号
     * @param id       目标 id
     * @param dto      目标内容
     * @return 更新后的目标视图
     * @throws com.xp.starsite.common.exception.BizException 目标不存在（400）或非本人目标（403）
     */
    GoalVO update(Long memberId, Long id, GoalSaveDTO dto);

    /**
     * 删除目标
     *
     * @param memberId 当前登录账号
     * @param id       目标 id
     * @throws com.xp.starsite.common.exception.BizException 目标不存在（400）或非本人目标（403）
     */
    void delete(Long memberId, Long id);
}
