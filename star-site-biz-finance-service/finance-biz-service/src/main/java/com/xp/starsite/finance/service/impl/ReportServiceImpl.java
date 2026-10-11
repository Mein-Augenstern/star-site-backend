package com.xp.starsite.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xp.starsite.api.client.ArchiveApi;
import com.xp.starsite.api.dto.AssetSummaryDTO;
import com.xp.starsite.finance.entity.FinanceAccount;
import com.xp.starsite.finance.entity.FinanceNetWorthSnapshot;
import com.xp.starsite.finance.entity.FinanceTransaction;
import com.xp.starsite.finance.mapper.FinanceAccountMapper;
import com.xp.starsite.finance.mapper.FinanceNetWorthSnapshotMapper;
import com.xp.starsite.finance.mapper.FinanceTransactionMapper;
import com.xp.starsite.finance.service.ReportService;
import com.xp.starsite.finance.vo.BalanceSheetVO;
import com.xp.starsite.finance.vo.CashFlowVO;
import com.xp.starsite.finance.vo.IncomeExpenseVO;
import com.xp.starsite.finance.vo.CategoryShareVO;
import com.xp.starsite.finance.vo.MonthlyTrendVO;
import com.xp.starsite.finance.vo.NetWorthPointVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 三张表实现。口径（见 技术/07）：
 * - 资产负债表：资产 = 资产账户余额 + archive 资产估值；负债 = 负债账户余额；报表为读场景，
 *   archive 不可用时 fail-open（archiveAsset=null + archiveAvailable=false），与归属校验的 fail-closed 区分。
 * - 收支：仅 direction 1/2 计入；转账/调整不计。
 * - 快照：每日 02:30 落一行（uk(owner,date) 幂等），曲线由此出。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final Set<String> LIQUID_TYPES = Set.of("cash", "bank", "wechat", "alipay");
    private static final int DIR_INCOME = 1;
    private static final int DIR_EXPENSE = 2;

    private final FinanceAccountMapper accountMapper;
    private final FinanceTransactionMapper transactionMapper;
    private final FinanceNetWorthSnapshotMapper snapshotMapper;
    private final ArchiveApi archiveApi;

    @Value("${finance.snapshot-cron:0 30 2 * * ?}")
    private String cronHint;

    @Override
    public BalanceSheetVO balanceSheet(Long memberId) {
        List<FinanceAccount> accounts = accountsOf(memberId);
        BigDecimal accountAsset = sumNonLiability(accounts);
        BigDecimal totalLiability = sumLiability(accounts);

        BalanceSheetVO vo = new BalanceSheetVO();
        vo.setAccountAsset(accountAsset);
        vo.setTotalLiability(totalLiability);
        try {
            BigDecimal archiveAsset = archiveApi.assetSummary(memberId).getData().getTotalValue();
            vo.setArchiveAsset(archiveAsset == null ? BigDecimal.ZERO : archiveAsset);
            vo.setArchiveAvailable(true);
        } catch (Exception e) {
            log.warn("archive 资产总览不可用，资产负债表按 0 展示档案资产: {}", e.getMessage());
            vo.setArchiveAsset(null);
            vo.setArchiveAvailable(false);
        }
        BigDecimal archivePart = Boolean.TRUE.equals(vo.getArchiveAvailable()) ? vo.getArchiveAsset() : BigDecimal.ZERO;
        vo.setTotalAsset(accountAsset.add(archivePart));
        vo.setNetWorth(vo.getTotalAsset().subtract(totalLiability));
        return vo;
    }

    @Override
    public IncomeExpenseVO incomeExpense(Long memberId, int year, int month) {
        LocalDateTime start;
        LocalDateTime end;
        if (month > 0) {
            YearMonth ym = YearMonth.of(year, month);
            start = ym.atDay(1).atStartOfDay();
            end = ym.plusMonths(1).atDay(1).atStartOfDay();
        } else {
            start = LocalDate.of(year, 1, 1).atStartOfDay();
            end = LocalDate.of(year + 1, 1, 1).atStartOfDay();
        }
        List<FinanceTransaction> txs = transactionMapper.selectList(
                new LambdaQueryWrapper<FinanceTransaction>()
                        .eq(FinanceTransaction::getOwnerMemberId, memberId)
                        .eq(FinanceTransaction::getDirection, DIR_INCOME)
                        .ge(FinanceTransaction::getTransTime, start)
                        .lt(FinanceTransaction::getTransTime, end));
        List<FinanceTransaction> expenses = transactionMapper.selectList(
                new LambdaQueryWrapper<FinanceTransaction>()
                        .eq(FinanceTransaction::getOwnerMemberId, memberId)
                        .eq(FinanceTransaction::getDirection, DIR_EXPENSE)
                        .ge(FinanceTransaction::getTransTime, start)
                        .lt(FinanceTransaction::getTransTime, end));

        BigDecimal income = txs.stream().map(FinanceTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = expenses.stream().map(FinanceTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        IncomeExpenseVO vo = new IncomeExpenseVO();
        vo.setYear(year);
        vo.setMonth(month > 0 ? month : null);
        vo.setIncome(income);
        vo.setExpense(expense);
        vo.setSurplus(income.subtract(expense));
        vo.setSurplusRate(income.signum() == 0 ? null
                : vo.getSurplus().divide(income, 4, RoundingMode.HALF_UP));
        return vo;
    }

    @Override
    public CashFlowVO cashFlow(Long memberId) {
        BigDecimal liquid = accountsOf(memberId).stream()
                .filter(a -> LIQUID_TYPES.contains(a.getAccountType()))
                .map(FinanceAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 近 12 个月月均支出
        LocalDateTime start = LocalDate.now().minusMonths(12).withDayOfMonth(1).atStartOfDay();
        BigDecimal yearExpense = transactionMapper.selectList(
                        new LambdaQueryWrapper<FinanceTransaction>()
                                .eq(FinanceTransaction::getOwnerMemberId, memberId)
                                .eq(FinanceTransaction::getDirection, DIR_EXPENSE)
                                .ge(FinanceTransaction::getTransTime, start))
                .stream().map(FinanceTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CashFlowVO vo = new CashFlowVO();
        vo.setLiquidBalance(liquid);
        vo.setMonthlyAvgExpense(yearExpense.signum() == 0 ? null
                : yearExpense.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP));
        vo.setMonthsCovered(vo.getMonthlyAvgExpense() == null || vo.getMonthlyAvgExpense().signum() == 0
                ? null
                : liquid.divide(vo.getMonthlyAvgExpense(), 1, RoundingMode.HALF_UP));
        return vo;
    }

    @Override
    public List<NetWorthPointVO> netWorthTrend(Long memberId) {
        return snapshotMapper.selectList(new LambdaQueryWrapper<FinanceNetWorthSnapshot>()
                        .eq(FinanceNetWorthSnapshot::getOwnerMemberId, memberId)
                        .orderByAsc(FinanceNetWorthSnapshot::getSnapDate))
                .stream().map(s -> {
                    NetWorthPointVO p = new NetWorthPointVO();
                    p.setDate(s.getSnapDate());
                    p.setNetWorth(s.getNetWorth());
                    return p;
                }).toList();
    }

    @Override
    public void snapshotToday(Long memberId) {
        BalanceSheetVO sheet = balanceSheet(memberId);
        LocalDate today = LocalDate.now();
        FinanceNetWorthSnapshot exist = snapshotMapper.selectOne(
                new LambdaQueryWrapper<FinanceNetWorthSnapshot>()
                        .eq(FinanceNetWorthSnapshot::getOwnerMemberId, memberId)
                        .eq(FinanceNetWorthSnapshot::getSnapDate, today));
        FinanceNetWorthSnapshot s = exist == null ? new FinanceNetWorthSnapshot() : exist;
        if (exist == null) {
            s.setOwnerMemberId(memberId);
            s.setSnapDate(today);
            s.setFamilyId(0L);
        }
        s.setTotalAsset(sheet.getTotalAsset());
        s.setTotalLiability(sheet.getTotalLiability());
        s.setNetWorth(sheet.getNetWorth());
        try {
            if (exist == null) {
                snapshotMapper.insert(s);
            } else {
                snapshotMapper.updateById(s);
            }
        } catch (DuplicateKeyException e) {
            log.debug("快照已存在，跳过: owner={}, date={}", memberId, today);
        }
    }

    @Override
    public List<CategoryShareVO> categoryShare(Long memberId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
        Map<String, BigDecimal> byCategory = transactionMapper.selectList(
                        new LambdaQueryWrapper<FinanceTransaction>()
                                .eq(FinanceTransaction::getOwnerMemberId, memberId)
                                .eq(FinanceTransaction::getDirection, DIR_EXPENSE)
                                .ge(FinanceTransaction::getTransTime, start)
                                .lt(FinanceTransaction::getTransTime, end))
                .stream()
                .filter(t -> t.getCategory() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        FinanceTransaction::getCategory,
                        java.util.stream.Collectors.reducing(BigDecimal.ZERO,
                                FinanceTransaction::getAmount, BigDecimal::add)));
        return byCategory.entrySet().stream()
                .map(e -> new CategoryShareVO(e.getKey(),
                        com.xp.starsite.finance.FinanceCategories.labelOf(e.getKey()), e.getValue()))
                .sorted(java.util.Comparator.comparing(CategoryShareVO::getAmount).reversed())
                .toList();
    }

    @Override
    public List<MonthlyTrendVO> monthlyTrend(Long memberId) {
        LocalDate startMonth = LocalDate.now().minusMonths(11).withDayOfMonth(1);
        LocalDateTime start = startMonth.atStartOfDay();
        LocalDateTime end = LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay();
        List<FinanceTransaction> txs = transactionMapper.selectList(
                new LambdaQueryWrapper<FinanceTransaction>()
                        .eq(FinanceTransaction::getOwnerMemberId, memberId)
                        .in(FinanceTransaction::getDirection, DIR_INCOME, DIR_EXPENSE)
                        .ge(FinanceTransaction::getTransTime, start)
                        .lt(FinanceTransaction::getTransTime, end));
        // YYYY-MM -> [income, expense]
        Map<String, BigDecimal[]> byMonth = new java.util.LinkedHashMap<>();
        for (FinanceTransaction t : txs) {
            String key = t.getTransTime().toLocalDate().withDayOfMonth(1).toString();
            BigDecimal[] pair = byMonth.computeIfAbsent(key,
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            int idx = t.getDirection() == DIR_INCOME ? 0 : 1;
            pair[idx] = pair[idx].add(t.getAmount());
        }
        List<MonthlyTrendVO> result = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            String key = startMonth.plusMonths(i).toString();
            BigDecimal[] pair = byMonth.getOrDefault(key, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            result.add(new MonthlyTrendVO(key, pair[0], pair[1]));
        }
        return result;
    }

    @Override
    public NetWorthPointVO snapshotNow(Long memberId) {
        snapshotToday(memberId);
        FinanceNetWorthSnapshot s = snapshotMapper.selectOne(
                new LambdaQueryWrapper<FinanceNetWorthSnapshot>()
                        .eq(FinanceNetWorthSnapshot::getOwnerMemberId, memberId)
                        .eq(FinanceNetWorthSnapshot::getSnapDate, LocalDate.now()));
        NetWorthPointVO vo = new NetWorthPointVO();
        vo.setDate(LocalDate.now());
        vo.setNetWorth(s.getNetWorth());
        return vo;
    }

    /** 每日快照：对全部有账户的 owner 各落一行（当前单实例 @Scheduled，多实例时再引锁——技术雷达后置项） */
    @Scheduled(cron = "${finance.snapshot-cron:0 30 2 * * ?}")
    public void dailySnapshot() {
        List<Long> owners = accountMapper.selectList(
                        new LambdaQueryWrapper<FinanceAccount>().select(FinanceAccount::getOwnerMemberId))
                .stream().map(FinanceAccount::getOwnerMemberId).distinct().toList();
        for (Long owner : owners) {
            try {
                snapshotToday(owner);
            } catch (Exception e) {
                // 单个 owner 失败不阻断整批
                log.error("净资产快照失败: owner={}, err={}", owner, e.getMessage());
            }
        }
        log.info("每日净资产快照完成: cron={}, owners={}", cronHint, owners.size());
    }

    private List<FinanceAccount> accountsOf(Long memberId) {
        return accountMapper.selectList(new LambdaQueryWrapper<FinanceAccount>()
                .eq(FinanceAccount::getOwnerMemberId, memberId));
    }

    private BigDecimal sumNonLiability(List<FinanceAccount> accounts) {
        return accounts.stream()
                .filter(a -> a.getLiabilityFlag() == null || a.getLiabilityFlag() == 0)
                .map(a -> a.getBalance() == null ? BigDecimal.ZERO : a.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumLiability(List<FinanceAccount> accounts) {
        return accounts.stream()
                .filter(a -> a.getLiabilityFlag() != null && a.getLiabilityFlag() == 1)
                .map(a -> a.getBalance() == null ? BigDecimal.ZERO : a.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
