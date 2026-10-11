package com.xp.starsite.finance.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.finance.dto.TransactionSaveDTO;
import com.xp.starsite.finance.service.TransactionService;
import com.xp.starsite.finance.vo.PageVO;
import com.xp.starsite.finance.vo.TransactionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 记账流水接口（全部需 JWT；数据按当前账号隔离）
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/finance/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public R<TransactionVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                   @Valid @RequestBody TransactionSaveDTO dto) {
        return R.ok(transactionService.create(memberId, dto));
    }

    @GetMapping
    public R<PageVO<TransactionVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                         @RequestParam(required = false) Long accountId,
                                         @RequestParam(required = false) Integer direction,
                                         @RequestParam(required = false) Integer year,
                                         @RequestParam(required = false) Integer month,
                                         @RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "20") long size) {
        int y = year == null ? java.time.LocalDate.now().getYear() : year;
        int m = month == null ? java.time.LocalDate.now().getMonthValue() : month;
        return R.ok(transactionService.list(memberId, accountId, direction, y, m, page, size));
    }

    @GetMapping("/{id}")
    public R<TransactionVO> detail(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                   @PathVariable Long id) {
        return R.ok(transactionService.getDetail(memberId, id));
    }

    /** 删除流水并回滚余额（转账整组回滚） */
    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        transactionService.delete(memberId, id);
        return R.ok();
    }
}
