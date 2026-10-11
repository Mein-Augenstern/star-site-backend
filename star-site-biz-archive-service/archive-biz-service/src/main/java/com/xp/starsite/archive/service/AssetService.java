package com.xp.starsite.archive.service;

import com.xp.starsite.archive.dto.AssetSaveDTO;
import com.xp.starsite.archive.vo.AssetSummaryVO;
import com.xp.starsite.archive.vo.AssetVO;
import com.xp.starsite.archive.vo.PageVO;
import com.xp.starsite.archive.vo.ValuationPointVO;

import java.util.List;

/**
 * 家庭资产服务（房产/车辆/存款理财台账）
 */
public interface AssetService {

    /** 创建资产（车辆带年检日 → 发年检提醒事件） */
    AssetVO create(Long memberId, AssetSaveDTO dto);

    /** 资产分页（筛选：类型） */
    PageVO<AssetVO> list(Long memberId, String assetType, long page, long size);

    /** 详情 */
    AssetVO getDetail(Long memberId, Long id);

    /** 更新（估值变更自动追加历史；年检日变更重发/撤销提醒） */
    AssetVO update(Long memberId, Long id, AssetSaveDTO dto);

    /** 删除（撤销年检提醒） */
    void delete(Long memberId, Long id);

    /** 估值历史（曲线数据，按时间升序） */
    List<ValuationPointVO> valuations(Long memberId, Long assetId);

    /** 资产总览：总估值 + 按类型分布 */
    AssetSummaryVO summary(Long memberId);
}
