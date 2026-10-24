package com.xp.starsite.finance.service.impl;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.common.util.IdGenerator;
import com.xp.starsite.finance.FinanceCategories;
import com.xp.starsite.finance.dto.BudgetSaveDTO;
import com.xp.starsite.finance.entity.FinanceBudget;
import com.xp.starsite.finance.entity.FinanceTransaction;
import com.xp.starsite.finance.repository.FinanceBudgetRepository;
import com.xp.starsite.finance.repository.FinanceTransactionRepository;
import com.xp.starsite.finance.service.BudgetService;
import com.xp.starsite.finance.vo.BudgetVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * 月度预算实现。使用率 = 当月该分类支出 / 预算（流水实时聚合，非快照）。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final FinanceBudgetRepository budgetRepository;
    private final FinanceTransactionRepository transactionRepository;
    private final IdGenerator idGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BudgetVO save(Long memberId, BudgetSaveDTO dto) {
        if (dto.getAmount().signum() <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "预算金额必须大于 0");
        }
        if (!FinanceCategories.isValid(dto.getCategory())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "不支持的分类");
        }
        YearMonth.parse(dto.getPeriod()); // 格式校验（非法抛 DateTimeParseException → 500 兜底）

        FinanceBudget budget = budgetRepository
                .findByOwnerCategoryPeriod(memberId, dto.getCategory(), dto.getPeriod())
                .orElse(null);
        if (budget == null) {
            budget = new FinanceBudget();
            budget.setId(idGenerator.nextId());
            budget.setOwnerMemberId(memberId);
            budget.setFamilyId(0L);
            budget.setCategory(dto.getCategory());
            budget.setPeriod(dto.getPeriod());
        }
        budget.setAmount(dto.getAmount());
        if (budget.getCreatedAt() == null) {
            budgetRepository.save(budget);
        } else {
            budgetRepository.update(budget);
        }
        return toVO(budget, spentOf(memberId, dto.getCategory(), dto.getPeriod()));
    }

    @Override
    public List<BudgetVO> list(Long memberId, String period) {
        return budgetRepository.listByOwnerPeriod(memberId, period).stream()
                .map(b -> toVO(b, spentOf(memberId, b.getCategory(), period)))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long memberId, Long id) {
        FinanceBudget budget = budgetRepository.findById(id).orElse(null);
        if (budget == null || !budget.getOwnerMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        budgetRepository.markDeleted(id);
    }

    /** 当月某分类支出合计（支出流水按月聚合） */
    private BigDecimal spentOf(Long memberId, String category, String period) {
        YearMonth ym = YearMonth.parse(period);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
        return transactionRepository.listByDirectionAndPeriod(memberId, 2, start, end, category).stream()
                .map(FinanceTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BudgetVO toVO(FinanceBudget budget, BigDecimal spent) {
        BudgetVO vo = new BudgetVO();
        vo.setId(budget.getId());
        vo.setCategory(budget.getCategory());
        vo.setCategoryName(FinanceCategories.labelOf(budget.getCategory()));
        vo.setPeriod(budget.getPeriod());
        vo.setAmount(budget.getAmount());
        vo.setSpent(spent);
        vo.setRemaining(budget.getAmount().subtract(spent));
        vo.setUsageRate(budget.getAmount().signum() == 0 ? null
                : spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP));
        return vo;
    }
}
