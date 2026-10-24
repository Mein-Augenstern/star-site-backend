package com.xp.starsite.finance.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.finance.controller.actions.FinanceActions;
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
 * <p>
 * 业务逻辑统一经模板包裹：模板包掉成功与失败的全部响应处理（成功 R.ok / 失败 R.fail），
 * Controller 一行返回；动作命名规范 finance.budget.*。
 *
 * @author muyi
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/finance/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 保存预算（同账期同分类 upsert）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param dto      预算内容（账期/分类/金额）
     * @return 保存后的预算视图（含使用进度）
     */
    @PostMapping
    public R<BudgetVO> save(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                            @Valid @RequestBody BudgetSaveDTO dto) {
        return writeTemplate.write(FinanceActions.BUDGET_SAVE,
                () -> budgetService.save(memberId, dto));
    }

    /**
     * 账期预算列表（含各预算使用进度）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param period   账期（如 2026-10）
     * @return 预算视图列表
     */
    @GetMapping
    public R<List<BudgetVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                  @RequestParam String period) {
        return queryTemplate.query(FinanceActions.BUDGET_LIST,
                () -> budgetService.list(memberId, period));
    }

    /**
     * 删除预算
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param id       预算 id
     * @return 删除成功（无业务数据）
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        return writeTemplate.write(FinanceActions.BUDGET_DELETE,
                () -> budgetService.delete(memberId, id));
    }
}
