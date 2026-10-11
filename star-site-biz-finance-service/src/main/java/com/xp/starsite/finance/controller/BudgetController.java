package com.xp.starsite.finance.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.finance.dto.BudgetSaveDTO;
import com.xp.starsite.finance.service.BudgetService;
import com.xp.starsite.finance.vo.BudgetVO;
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

import java.util.List;

/**
 * 月度预算接口（全部需 JWT）
 */
@RestController
@RequestMapping("/api/finance/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public R<BudgetVO> save(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                            @Valid @RequestBody BudgetSaveDTO dto) {
        return R.ok(budgetService.save(memberId, dto));
    }

    @GetMapping
    public R<List<BudgetVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                  @RequestParam String period) {
        return R.ok(budgetService.list(memberId, period));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        budgetService.delete(memberId, id);
        return R.ok();
    }
}
