package com.xp.starsite.finance.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
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
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/finance/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public R<AccountVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                               @Valid @RequestBody AccountSaveDTO dto) {
        return R.ok(accountService.create(memberId, dto));
    }

    @GetMapping
    public R<List<AccountVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(accountService.list(memberId));
    }

    @GetMapping("/{id}")
    public R<AccountVO> detail(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                               @PathVariable Long id) {
        return R.ok(accountService.getDetail(memberId, id));
    }

    @PutMapping("/{id}")
    public R<AccountVO> update(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                               @PathVariable Long id,
                               @Valid @RequestBody AccountSaveDTO dto) {
        return R.ok(accountService.update(memberId, id, dto));
    }

    /** 注销账户（余额须为 0） */
    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        accountService.delete(memberId, id);
        return R.ok();
    }
}
