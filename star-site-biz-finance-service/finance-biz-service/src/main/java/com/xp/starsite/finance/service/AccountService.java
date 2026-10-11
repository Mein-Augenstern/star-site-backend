package com.xp.starsite.finance.service;

import com.xp.starsite.finance.dto.AccountSaveDTO;
import com.xp.starsite.finance.vo.AccountVO;

import java.util.List;

/**
 * 资金账户服务。首参 memberId 用于归属校验。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface AccountService {

    /** 建户（初始余额 > 0 时自动记一笔调整流水留痕） */
    AccountVO create(Long memberId, AccountSaveDTO dto);

    /** 账户列表（含余额） */
    List<AccountVO> list(Long memberId);

    /** 详情 */
    AccountVO getDetail(Long memberId, Long id);

    /** 编辑（名称/机构/备注；余额不走这里，走调整流水） */
    AccountVO update(Long memberId, Long id, AccountSaveDTO dto);

    /** 注销账户（逻辑删；余额不为 0 时拒绝，防资产凭空消失） */
    void delete(Long memberId, Long id);
}
