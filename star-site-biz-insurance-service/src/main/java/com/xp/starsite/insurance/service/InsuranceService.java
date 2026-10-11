package com.xp.starsite.insurance.service;

import com.xp.starsite.insurance.dto.PolicySaveDTO;
import com.xp.starsite.insurance.vo.CoverageMapVO;
import com.xp.starsite.insurance.vo.PageVO;
import com.xp.starsite.insurance.vo.PolicyVO;

/**
 * 保单服务。方法首参 memberId 为当前登录顶梁柱，用于归属校验（防横向越权）。
 */
public interface InsuranceService {

    /** 创建保单（校验被保人归属；发缴费/到期提醒事件） */
    PolicyVO create(Long memberId, PolicySaveDTO dto);

    /** 保单分页（范围收口在本账号） */
    PageVO<PolicyVO> list(Long memberId, Long insuredMemberId, String category, Integer status,
                          long page, long size);

    /** 详情（他人保单 403） */
    PolicyVO getDetail(Long memberId, Long id);

    /** 更新保单（重发提醒事件，upsert 幂等） */
    PolicyVO update(Long memberId, Long id, PolicySaveDTO dto);

    /** 删除保单（逻辑删 + 撤销提醒规则） */
    void delete(Long memberId, Long id);

    /** 保障地图：家庭成员 × 险种覆盖矩阵 */
    CoverageMapVO coverageMap(Long memberId);
}
