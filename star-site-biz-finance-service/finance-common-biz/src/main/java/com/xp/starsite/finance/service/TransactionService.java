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

    /**
     * 记账（收入/支出/转账/调整），流水与余额同事务
     *
     * @param memberId 当前登录账号
     * @param dto      记账内容（类型/账户/金额/方向等）
     * @return 新建流水视图
     * @throws com.xp.starsite.common.exception.BizException 记账类型不支持、金额非法、
     *                                                       转入账户缺失/相同、账户不存在或已注销（400）；
     *                                                       账户非本人（403）
     */
    TransactionVO create(Long memberId, TransactionSaveDTO dto);

    /**
     * 流水分页（可按账户/方向筛选，时间倒序；year/month 缺省取当前年月）
     *
     * @param memberId  当前登录账号
     * @param accountId 可选，按账户过滤
     * @param direction 可选，按方向过滤（1 收入 / 2 支出 / 3 转账 / 4 调整）
     * @param year      可空，年份（缺省当年）
     * @param month     可空，月份（缺省当月；传 0 = 全年）
     * @param page      页码，从 1 起
     * @param size      每页条数
     * @return 流水分页
     */
    PageVO<TransactionVO> list(Long memberId, Long accountId, Integer direction,
                               Integer year, Integer month, long page, long size);

    /**
     * 流水详情
     *
     * @param memberId 当前登录账号
     * @param id       流水 id
     * @return 流水视图
     * @throws com.xp.starsite.common.exception.BizException 流水不存在（400）或非本人流水（403）
     */
    TransactionVO getDetail(Long memberId, Long id);

    /**
     * 删除流水并回滚余额（转账删一笔自动整组回滚）。
     * V1 不支持编辑金额——删了重记，保证余额推导链清晰。
     *
     * @param memberId 当前登录账号
     * @param id       流水 id
     * @throws com.xp.starsite.common.exception.BizException 流水不存在（400）或非本人流水（403）
     */
    void delete(Long memberId, Long id);
}
