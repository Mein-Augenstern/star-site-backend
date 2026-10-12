package com.xp.starsite.insurance.service;

import com.xp.starsite.insurance.dto.PolicySaveDTO;
import com.xp.starsite.insurance.vo.CoverageMapVO;
import com.xp.starsite.insurance.vo.PageVO;
import com.xp.starsite.insurance.vo.PolicyVO;

/**
 * 保单服务。方法首参 memberId 为当前登录顶梁柱，用于归属校验（防横向越权）。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface InsuranceService {

    /**
     * 创建保单（校验被保人归属；发缴费/到期提醒事件）
     *
     * @param memberId 当前登录账号
     * @param dto      保单内容（险种/被保人/缴费与到期日等）
     * @return 新建保单视图
     * @throws com.xp.starsite.common.exception.BizException 被保人不属于本家庭（403）、
     *                                                       不支持的险种类型（400）
     */
    PolicyVO create(Long memberId, PolicySaveDTO dto);

    /**
     * 保单分页（范围收口在本账号）
     *
     * @param memberId         当前登录账号
     * @param insuredMemberId  可选，按被保人过滤（须属于本家庭）
     * @param category         可选，按险种过滤
     * @param status           可选，按状态过滤
     * @param page             页码，从 1 起
     * @param size             每页条数
     * @return 保单分页
     * @throws com.xp.starsite.common.exception.BizException 指定被保人不属于本家庭（403）
     */
    PageVO<PolicyVO> list(Long memberId, Long insuredMemberId, String category, Integer status,
                          long page, long size);

    /**
     * 详情（他人保单 403）
     *
     * @param memberId 当前登录账号
     * @param id       保单 id
     * @return 保单视图
     * @throws com.xp.starsite.common.exception.BizException 保单不存在（400）或非本账号保单（403）
     */
    PolicyVO getDetail(Long memberId, Long id);

    /**
     * 更新保单（重发提醒事件，upsert 幂等）
     *
     * @param memberId 当前登录账号
     * @param id       保单 id
     * @param dto      保单内容
     * @return 更新后的保单视图
     * @throws com.xp.starsite.common.exception.BizException 保单不存在（400）、非本账号保单（403）、
     *                                                       被保人不属于本家庭（403）、不支持的险种类型（400）
     */
    PolicyVO update(Long memberId, Long id, PolicySaveDTO dto);

    /**
     * 删除保单（逻辑删 + 撤销提醒规则）
     *
     * @param memberId 当前登录账号
     * @param id       保单 id
     * @throws com.xp.starsite.common.exception.BizException 保单不存在（400）或非本账号保单（403）
     */
    void delete(Long memberId, Long id);

    /**
     * 保障地图：家庭成员 × 险种覆盖矩阵
     *
     * @param memberId 当前登录账号
     * @return 保障地图视图
     */
    CoverageMapVO coverageMap(Long memberId);
}
