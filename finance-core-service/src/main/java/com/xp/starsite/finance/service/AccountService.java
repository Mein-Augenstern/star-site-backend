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

    /**
     * 建户（初始余额 > 0 时自动记一笔调整流水留痕）
     *
     * @param memberId 当前登录账号
     * @param dto      账户信息（名称/类型/机构/初始余额等）
     * @return 新建账户视图（含余额）
     * @throws com.xp.starsite.common.exception.BizException 不支持的账户类型（400）
     */
    AccountVO create(Long memberId, AccountSaveDTO dto);

    /**
     * 账户列表（含余额）
     *
     * @param memberId 当前登录账号
     * @return 全部在用账户视图
     */
    List<AccountVO> list(Long memberId);

    /**
     * 详情
     *
     * @param memberId 当前登录账号
     * @param id       账户 id
     * @return 账户视图
     * @throws com.xp.starsite.common.exception.BizException 账户不存在（400）或非本人账户（403）
     */
    AccountVO getDetail(Long memberId, Long id);

    /**
     * 编辑（名称/机构/备注；余额不走这里，走调整流水）
     *
     * @param memberId 当前登录账号
     * @param id       账户 id
     * @param dto      账户信息
     * @return 更新后的账户视图
     * @throws com.xp.starsite.common.exception.BizException 账户不存在（400）、非本人账户（403）、
     *                                                       不支持的账户类型（400）
     */
    AccountVO update(Long memberId, Long id, AccountSaveDTO dto);

    /**
     * 注销账户（逻辑删；余额不为 0 时拒绝，防资产凭空消失）
     *
     * @param memberId 当前登录账号
     * @param id       账户 id
     * @throws com.xp.starsite.common.exception.BizException 账户不存在（400）、非本人账户（403）、
     *                                                       余额不为 0 需先清零（400）
     */
    void delete(Long memberId, Long id);
}
