package com.xp.starsite.finance;

import com.xp.starsite.api.client.ArchiveApi;
import com.xp.starsite.api.dto.AssetSummaryDTO;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.R;
import com.xp.starsite.finance.entity.FinanceAccount;
import com.xp.starsite.finance.entity.FinanceNetWorthSnapshot;
import com.xp.starsite.finance.entity.FinanceTransaction;
import com.xp.starsite.finance.mapper.FinanceAccountMapper;
import com.xp.starsite.finance.mapper.FinanceNetWorthSnapshotMapper;
import com.xp.starsite.finance.mapper.FinanceTransactionMapper;
import com.xp.starsite.finance.service.impl.ReportServiceImpl;
import com.xp.starsite.finance.vo.BalanceSheetVO;
import com.xp.starsite.finance.vo.IncomeExpenseVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 三张表单元测试：资产负债聚合、archive 降级、收支口径、快照幂等（mock 全部依赖）
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final long ME = 1L;

    @Mock
    private FinanceAccountMapper accountMapper;
    @Mock
    private FinanceTransactionMapper transactionMapper;
    @Mock
    private FinanceNetWorthSnapshotMapper snapshotMapper;
    @Mock
    private ArchiveApi archiveApi;

    private ReportServiceImpl service() {
        return new ReportServiceImpl(accountMapper, transactionMapper, snapshotMapper, archiveApi);
    }

    private FinanceAccount account(long id, String type, String balance) {
        FinanceAccount a = new FinanceAccount();
        a.setId(id);
        a.setOwnerMemberId(ME);
        a.setAccountType(type);
        a.setBalance(new BigDecimal(balance));
        a.setLiabilityFlag(type.equals("credit") || type.equals("loan") ? 1 : 0);
        return a;
    }

    @Test
    void balanceSheetAggregatesAccountsAndArchive() {
        when(accountMapper.selectList(any())).thenReturn(List.of(
                account(1, "wechat", "5000"),
                account(2, "bank", "100000"),
                account(3, "credit", "8000")));
        when(archiveApi.assetSummary(ME)).thenReturn(R.ok(new AssetSummaryDTO(new BigDecimal("2000000"))));

        BalanceSheetVO vo = service().balanceSheet(ME);

        // 资产 = 5000+100000（账户）+2000000（档案）；负债 = 8000（信用卡）
        assertEquals(0, vo.getAccountAsset().compareTo(new BigDecimal("105000")));
        assertEquals(0, vo.getArchiveAsset().compareTo(new BigDecimal("2000000")));
        assertTrue(vo.getArchiveAvailable());
        assertEquals(0, vo.getTotalAsset().compareTo(new BigDecimal("2105000")));
        assertEquals(0, vo.getTotalLiability().compareTo(new BigDecimal("8000")));
        assertEquals(0, vo.getNetWorth().compareTo(new BigDecimal("2097000")));
    }

    @Test
    void balanceSheetFailsOpenWhenArchiveDown() {
        when(accountMapper.selectList(any())).thenReturn(List.of(account(1, "wechat", "5000")));
        when(archiveApi.assetSummary(ME)).thenThrow(new BizException(500, "archive 不可用"));

        BalanceSheetVO vo = service().balanceSheet(ME);

        // 报表为读场景 fail-open：档案资产置 null 并标注不可用，账户资产照常展示
        assertFalse(vo.getArchiveAvailable());
        assertNull(vo.getArchiveAsset());
        assertEquals(0, vo.getTotalAsset().compareTo(new BigDecimal("5000")));
    }

    @Test
    void incomeExpenseOnlyCountsDirection12() {
        when(transactionMapper.selectList(any()))
                .thenReturn(List.of(tx(1, "10000"), tx(1, "2000")))
                .thenReturn(List.of(tx(2, "3000"), tx(2, "1000")));

        IncomeExpenseVO vo = service().incomeExpense(ME, 2026, 10);

        assertEquals(0, vo.getIncome().compareTo(new BigDecimal("12000")));
        assertEquals(0, vo.getExpense().compareTo(new BigDecimal("4000")));
        assertEquals(0, vo.getSurplus().compareTo(new BigDecimal("8000")));
        // 结余率 8000/12000 = 0.6667
        assertEquals(0, vo.getSurplusRate().compareTo(new BigDecimal("0.6667")));
    }

    @Test
    void snapshotIsIdempotentPerDay() {
        when(accountMapper.selectList(any())).thenReturn(List.of(account(1, "wechat", "5000")));
        when(archiveApi.assetSummary(ME)).thenReturn(R.ok(new AssetSummaryDTO(BigDecimal.ZERO)));
        when(snapshotMapper.selectOne(any())).thenReturn(null);

        service().snapshotToday(ME);

        ArgumentCaptor<FinanceNetWorthSnapshot> cap =
                ArgumentCaptor.forClass(FinanceNetWorthSnapshot.class);
        verify(snapshotMapper).insert(cap.capture());
        assertEquals(LocalDate.now(), cap.getValue().getSnapDate());
        assertEquals(0, cap.getValue().getNetWorth().compareTo(new BigDecimal("5000")));
    }

    @Test
    void snapshotDuplicateDaySkipsInsert() {
        when(accountMapper.selectList(any())).thenReturn(List.of(account(1, "wechat", "5000")));
        when(archiveApi.assetSummary(ME)).thenReturn(R.ok(new AssetSummaryDTO(BigDecimal.ZERO)));
        // 当日快照已存在
        FinanceNetWorthSnapshot exist = new FinanceNetWorthSnapshot();
        exist.setOwnerMemberId(ME);
        exist.setSnapDate(LocalDate.now());
        when(snapshotMapper.selectOne(any())).thenReturn(exist);

        service().snapshotToday(ME);

        // 已有则 update 而非 insert
        verify(snapshotMapper).updateById(any(FinanceNetWorthSnapshot.class));
        verify(snapshotMapper, never()).insert(any(FinanceNetWorthSnapshot.class));
    }

    private FinanceTransaction tx(int direction, String amount) {
        FinanceTransaction t = new FinanceTransaction();
        t.setOwnerMemberId(ME);
        t.setDirection(direction);
        t.setAmount(new BigDecimal(amount));
        t.setCategory("FOOD");
        t.setTransTime(LocalDateTime.now());
        return t;
    }

    @Test
    void categoryShareGroupsByCategoryDesc() {
        List<FinanceTransaction> rows = List.of(tx(2, "60"), tx(2, "40"), tx(2, "20"));
        rows.get(2).setCategory("CAR");
        rows.get(2).setCategoryName("用车");
        when(transactionMapper.selectList(any())).thenReturn(rows);

        var shares = service().categoryShare(ME, 2026, 10);

        assertEquals(2, shares.size());
        assertEquals("FOOD", shares.get(0).getCategory());
        assertEquals(0, shares.get(0).getAmount().compareTo(new BigDecimal("100")));
        assertEquals("CAR", shares.get(1).getCategory());
        assertEquals(0, shares.get(1).getAmount().compareTo(new BigDecimal("20")));
    }

    @Test
    void monthlyTrendFillsEmptyMonths() {
        when(transactionMapper.selectList(any()))
                .thenReturn(List.of(tx(1, "10000"), tx(2, "3000")));

        var trend = service().monthlyTrend(ME);

        // 近 12 个月全量返回（空月补零）
        assertEquals(12, trend.size());
        var current = trend.get(11);
        assertEquals(0, current.getIncome().compareTo(new BigDecimal("10000")));
        assertEquals(0, current.getExpense().compareTo(new BigDecimal("3000")));
        assertEquals(0, trend.get(0).getIncome().compareTo(BigDecimal.ZERO));
    }

    @Test
    void snapshotNowPersistsAndReturns() {
        when(accountMapper.selectList(any())).thenReturn(List.of(account(1, "wechat", "5000")));
        when(archiveApi.assetSummary(ME)).thenReturn(R.ok(new AssetSummaryDTO(BigDecimal.ZERO)));
        // selectOne 首次（snapshotToday 内）无快照，insert 后（返回值查询）有
        java.util.concurrent.atomic.AtomicReference<FinanceNetWorthSnapshot> inserted =
                new java.util.concurrent.atomic.AtomicReference<>();
        when(snapshotMapper.insert(any(FinanceNetWorthSnapshot.class))).thenAnswer(inv -> {
            inserted.set(inv.getArgument(0));
            return 1;
        });
        when(snapshotMapper.selectOne(any())).thenAnswer(inv -> inserted.get());

        var point = service().snapshotNow(ME);

        verify(snapshotMapper).insert(any(FinanceNetWorthSnapshot.class));
        assertEquals(LocalDate.now(), point.getDate());
        assertEquals(0, point.getNetWorth().compareTo(new BigDecimal("5000")));
    }
}
