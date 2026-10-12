package com.xp.starsite.finance.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.finance.controller.actions.FinanceActions;
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
 * <p>
 * 业务逻辑统一经模板包裹：模板包掉成功与失败的全部响应处理（成功 R.ok / 失败 R.fail），
 * Controller 一行返回；动作命名规范 finance.transaction.*。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/finance/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 记一笔流水（收支/转账；联动账户余额）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param dto      流水内容（账户/方向/金额/分类等）
     * @return 新建流水视图
     */
    @PostMapping
    public R<TransactionVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                   @Valid @RequestBody TransactionSaveDTO dto) {
        return writeTemplate.write(FinanceActions.TRANSACTION_CREATE,
                () -> transactionService.create(memberId, dto));
    }

    /**
     * 流水分页（按账期倒序；year/month 缺省取当月，归一化在 TransactionService 内完成）
     *
     * @param memberId  当前登录账号（JWT 注入）
     * @param accountId 可选，按账户过滤
     * @param direction 可选，按方向过滤（1 支出 / 2 收入）
     * @param year      可选，年份（缺省当年）
     * @param month     可选，月份（缺省当月）
     * @param page      页码（从 1 起，默认 1）
     * @param size      每页条数（默认 20）
     * @return 流水分页
     */
    @GetMapping
    public R<PageVO<TransactionVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                         @RequestParam(required = false) Long accountId,
                                         @RequestParam(required = false) Integer direction,
                                         @RequestParam(required = false) Integer year,
                                         @RequestParam(required = false) Integer month,
                                         @RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "20") long size) {
        return queryTemplate.query(FinanceActions.TRANSACTION_PAGE,
                () -> transactionService.list(memberId, accountId, direction, year, month, page, size));
    }

    /**
     * 流水详情
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param id       流水 id
     * @return 流水视图
     */
    @GetMapping("/{id}")
    public R<TransactionVO> detail(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                   @PathVariable Long id) {
        return queryTemplate.query(FinanceActions.TRANSACTION_DETAIL,
                () -> transactionService.getDetail(memberId, id));
    }

    /**
     * 删除流水并回滚余额（转账整组回滚）
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param id       流水 id
     * @return 删除成功（无业务数据）
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        return writeTemplate.write(FinanceActions.TRANSACTION_DELETE,
                () -> transactionService.delete(memberId, id));
    }
}
