package com.xp.starsite.archive.service;

import com.xp.starsite.archive.dto.AssetSaveDTO;
import com.xp.starsite.archive.vo.AssetSummaryVO;
import com.xp.starsite.archive.vo.AssetVO;
import com.xp.starsite.archive.vo.PageVO;
import com.xp.starsite.archive.vo.ValuationPointVO;

import java.util.List;

/**
 * 家庭资产服务（房产/车辆/存款理财台账）
 *
 * @author muyi
 * @since 2026-10
 */
public interface AssetService {

    /**
     * 创建资产（车辆带年检日 → 发年检提醒事件）
     *
     * @param memberId 当前登录账号
     * @param dto      资产内容（类型/名称/估值/归属成员等）
     * @return 新建资产视图
     * @throws com.xp.starsite.common.exception.BizException 归属成员不属于本家庭（403）、
     *                                                       不支持的资产类型（400）
     */
    AssetVO create(Long memberId, AssetSaveDTO dto);

    /**
     * 资产分页（筛选：类型）
     *
     * @param memberId  当前登录账号
     * @param assetType 可选，按资产类型过滤
     * @param page      页码，从 1 起
     * @param size      每页条数
     * @return 资产分页
     */
    PageVO<AssetVO> list(Long memberId, String assetType, long page, long size);

    /**
     * 详情
     *
     * @param memberId 当前登录账号
     * @param id       资产 id
     * @return 资产视图
     * @throws com.xp.starsite.common.exception.BizException 资产不存在（400）或非本人资产（403）
     */
    AssetVO getDetail(Long memberId, Long id);

    /**
     * 更新（估值变更自动追加历史；年检日变更重发/撤销提醒）
     *
     * @param memberId 当前登录账号
     * @param id       资产 id
     * @param dto      资产内容
     * @return 更新后的资产视图
     * @throws com.xp.starsite.common.exception.BizException 资产不存在（400）、非本人资产（403）、
     *                                                       归属成员不属于本家庭（403）、不支持的资产类型（400）
     */
    AssetVO update(Long memberId, Long id, AssetSaveDTO dto);

    /**
     * 删除（撤销年检提醒）
     *
     * @param memberId 当前登录账号
     * @param id       资产 id
     * @throws com.xp.starsite.common.exception.BizException 资产不存在（400）或非本人资产（403）
     */
    void delete(Long memberId, Long id);

    /**
     * 估值历史（曲线数据，按时间升序）
     *
     * @param memberId 当前登录账号
     * @param assetId  资产 id
     * @return 估值历史点列表
     * @throws com.xp.starsite.common.exception.BizException 资产不存在（400）或非本人资产（403）
     */
    List<ValuationPointVO> valuations(Long memberId, Long assetId);

    /**
     * 资产总览：总估值 + 按类型分布
     *
     * @param memberId 当前登录账号
     * @return 资产总览视图
     */
    AssetSummaryVO summary(Long memberId);
}
