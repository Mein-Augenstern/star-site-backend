package com.xp.starsite.finance.service;

import com.xp.starsite.finance.dto.TransactionSaveDTO;
import com.xp.starsite.finance.vo.PageVO;
import com.xp.starsite.finance.vo.TransactionVO;

/**
 * 记账流水服务。首参 memberId 用于归属校验。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface TransactionService {

    /** 记账（收入/支出/转账/调整），流水与余额同事务 */
    TransactionVO create(Long memberId, TransactionSaveDTO dto);

    /** 流水分页（可按账户/方向筛选，时间倒序） */
    PageVO<TransactionVO> list(Long memberId, Long accountId, Integer direction,
                               int year, int month, long page, long size);

    /** 流水详情 */
    TransactionVO getDetail(Long memberId, Long id);

    /**
     * 删除流水并回滚余额（转账删一笔自动整组回滚）。
     * V1 不支持编辑金额——删了重记，保证余额推导链清晰。
     */
    void delete(Long memberId, Long id);
}
