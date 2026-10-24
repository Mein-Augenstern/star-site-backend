package com.xp.starsite.finance.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.finance.controller.actions.FinanceActions;
import com.xp.starsite.finance.dto.AccountSaveDTO;
import com.xp.starsite.finance.service.AccountService;
import com.xp.starsite.finance.vo.AccountVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 资金账户接口（全部需 JWT；数据按当前账号隔离）
 * <p>
 * 业务逻辑统一经模板包裹：模板包掉成功与失败的全部响应处理（成功 R.ok / 失败 R.fail），
 * Controller 一行返回；动作命名规范 finance.account.*。
 *
 * @author muyi
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/finance/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 新建资金账户
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param dto      账户信息（名称/类型/初始余额等）
     * @return 新建账户视图
     */
    @PostMapping
    public R<AccountVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                               @Valid @RequestBody AccountSaveDTO dto) {
        return writeTemplate.write(FinanceActions.ACCOUNT_CREATE,
                () -> accountService.create(memberId, dto));
    }

    /**
     * 我的账户列表（含各账户余额）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @return 账户视图列表
     */
    @GetMapping
    public R<List<AccountVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query(FinanceActions.ACCOUNT_LIST, () -> accountService.list(memberId));
    }

    /**
     * 账户详情
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param id       账户 id
     * @return 账户视图
     */
    @GetMapping("/{id}")
    public R<AccountVO> detail(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                               @PathVariable Long id) {
        return queryTemplate.query(FinanceActions.ACCOUNT_DETAIL,
                () -> accountService.getDetail(memberId, id));
    }

    /**
     * 修改账户信息
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param id       账户 id
     * @param dto      账户更新内容
     * @return 更新后的账户视图
     */
    @PutMapping("/{id}")
    public R<AccountVO> update(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                               @PathVariable Long id,
                               @Valid @RequestBody AccountSaveDTO dto) {
        return writeTemplate.write(FinanceActions.ACCOUNT_UPDATE,
                () -> accountService.update(memberId, id, dto));
    }

    /**
     * 注销账户（余额须为 0）
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param id       账户 id
     * @return 注销成功（无业务数据）
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        return writeTemplate.write(FinanceActions.ACCOUNT_DELETE,
                () -> accountService.delete(memberId, id));
    }
}
