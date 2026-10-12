package com.xp.starsite.finance;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.util.IdGenerator;
import com.xp.starsite.finance.dto.AccountSaveDTO;
import com.xp.starsite.finance.dto.TransactionSaveDTO;
import com.xp.starsite.finance.entity.FinanceAccount;
import com.xp.starsite.finance.entity.FinanceTransaction;
import com.xp.starsite.finance.repository.FinanceAccountRepository;
import com.xp.starsite.finance.repository.FinanceTransactionRepository;
import com.xp.starsite.finance.service.impl.AccountServiceImpl;
import com.xp.starsite.finance.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 财务域单元测试：余额联动、转账双流水、删除回滚、归属 403（mock 全部依赖，不连库）
 *
 * @author xiaopu
 * @since 2026-10
 */
@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    private static final long ME = 1L;
    private static final long OTHER = 2L;

    @Mock
    private FinanceAccountRepository accountRepository;
    @Mock
    private FinanceTransactionRepository transactionRepository;
    @Mock
    private IdGenerator idGenerator;

    /** EventPublisher 在 F1 财务域未使用（AccountServiceImpl 不依赖），占位满足构造 */
    private AccountServiceImpl accountService() {
        return new AccountServiceImpl(accountRepository, transactionRepository, idGenerator);
    }

    private TransactionServiceImpl txService() {
        return new TransactionServiceImpl(accountRepository, transactionRepository, idGenerator);
    }

    private FinanceAccount account(long id, BigDecimal balance) {
        FinanceAccount a = new FinanceAccount();
        a.setId(id);
        a.setOwnerMemberId(ME);
        a.setFamilyId(100L);
        a.setName("微信钱包");
        a.setAccountType("wechat");
        a.setBalance(balance);
        a.setInitialBalance(BigDecimal.ZERO);
        a.setLiabilityFlag(0);
        return a;
    }

    private TransactionSaveDTO dto(int direction, long accountId, String amount) {
        TransactionSaveDTO dto = new TransactionSaveDTO();
        dto.setDirection(direction);
        dto.setAccountId(accountId);
        dto.setAmount(new BigDecimal(amount));
        dto.setCategory("FOOD");
        dto.setTransTime(LocalDateTime.now());
        return dto;
    }

    /** 模拟仓储余额原子增量（一锁二判三更新）：余额在「库」里加 delta，重查可见；账户不存在返回 false */
    private void atomicBalanceStub() {
        when(accountRepository.updateBalanceDelta(anyLong(), any(BigDecimal.class))).thenAnswer(inv -> {
            FinanceAccount a = accountRepository.findById(inv.getArgument(0, Long.class)).orElse(null);
            if (a == null) {
                return false;
            }
            a.setBalance(a.getBalance().add(inv.getArgument(1, BigDecimal.class)));
            return true;
        });
    }

    /* ===== 记账与余额联动 ===== */

    @Test
    void incomeIncreasesBalanceWithSnapshot() {
        FinanceAccount acc = account(11L, new BigDecimal("100"));
        when(accountRepository.findById(11L)).thenReturn(Optional.of(acc));
        atomicBalanceStub();

        txService().create(ME, dto(1, 11L, "50"));

        assertEquals(0, acc.getBalance().compareTo(new BigDecimal("150")));
        ArgumentCaptor<FinanceTransaction> cap = ArgumentCaptor.forClass(FinanceTransaction.class);
        verify(transactionRepository).save(cap.capture());
        assertEquals(0, cap.getValue().getBalanceAfter().compareTo(new BigDecimal("150")));
    }

    @Test
    void expenseDecreasesBalance() {
        FinanceAccount acc = account(11L, new BigDecimal("100"));
        when(accountRepository.findById(11L)).thenReturn(Optional.of(acc));
        atomicBalanceStub();

        txService().create(ME, dto(2, 11L, "30"));

        assertEquals(0, acc.getBalance().compareTo(new BigDecimal("70")));
    }

    @Test
    void nonPositiveAmountRejected() {
        when(accountRepository.findById(11L)).thenReturn(Optional.of(account(11L, new BigDecimal("100"))));

        assertThrows(BizException.class, () -> txService().create(ME, dto(2, 11L, "0")));
        verify(transactionRepository, never()).save(any(FinanceTransaction.class));
    }

    /* ===== 转账 ===== */

    @Test
    void transferCreatesPairedTransactionsAndMovesBalance() {
        FinanceAccount from = account(11L, new BigDecimal("100"));
        FinanceAccount to = account(22L, new BigDecimal("0"));
        to.setName("招商银行卡");
        to.setAccountType("bank");
        when(accountRepository.findById(11L)).thenReturn(Optional.of(from));
        when(accountRepository.findById(22L)).thenReturn(Optional.of(to));
        atomicBalanceStub();

        TransactionSaveDTO dto = dto(3, 11L, "40");
        dto.setToAccountId(22L);
        txService().create(ME, dto);

        assertEquals(0, from.getBalance().compareTo(new BigDecimal("60")));
        assertEquals(0, to.getBalance().compareTo(new BigDecimal("40")));
        // 出+入两笔流水
        ArgumentCaptor<FinanceTransaction> cap = ArgumentCaptor.forClass(FinanceTransaction.class);
        verify(transactionRepository, times(2)).save(cap.capture());
        List<FinanceTransaction> legs = cap.getAllValues();
        assertEquals(legs.get(0).getTransferGroupId(), legs.get(1).getTransferGroupId());
    }

    @Test
    void transferToSameAccountRejected() {
        when(accountRepository.findById(11L)).thenReturn(Optional.of(account(11L, new BigDecimal("100"))));

        TransactionSaveDTO dto = dto(3, 11L, "40");
        dto.setToAccountId(11L);
        assertThrows(BizException.class, () -> txService().create(ME, dto));
    }

    /* ===== 删除回滚 ===== */

    @Test
    void deleteExpenseRollsBackBalance() {
        FinanceTransaction t = new FinanceTransaction();
        t.setId(5L);
        t.setOwnerMemberId(ME);
        t.setAccountId(11L);
        t.setDirection(2);
        t.setAmount(new BigDecimal("30"));
        t.setBalanceAfter(new BigDecimal("70"));
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(t));
        FinanceAccount acc = account(11L, new BigDecimal("70"));
        when(accountRepository.findById(11L)).thenReturn(Optional.of(acc));
        atomicBalanceStub();

        txService().delete(ME, 5L);

        // 支出删除 → 余额回加
        assertEquals(0, acc.getBalance().compareTo(new BigDecimal("100")));
        verify(transactionRepository).markDeleted(5L);
    }

    @Test
    void deleteTransferRollsBackBothLegs() {
        // 新模型：转账腿 amount 带符号——出腿负、入腿正
        FinanceTransaction leg1 = new FinanceTransaction();
        leg1.setId(6L);
        leg1.setOwnerMemberId(ME);
        leg1.setAccountId(11L);
        leg1.setDirection(3);
        leg1.setAmount(new BigDecimal("-40"));
        leg1.setBalanceAfter(new BigDecimal("60"));
        leg1.setTransferGroupId(999L);
        FinanceTransaction leg2 = new FinanceTransaction();
        leg2.setId(7L);
        leg2.setOwnerMemberId(ME);
        leg2.setAccountId(22L);
        leg2.setDirection(3);
        leg2.setAmount(new BigDecimal("40"));
        leg2.setBalanceAfter(new BigDecimal("40"));
        leg2.setTransferGroupId(999L);
        when(transactionRepository.findById(6L)).thenReturn(Optional.of(leg1));
        when(transactionRepository.findByTransferGroupId(999L)).thenReturn(List.of(leg1, leg2));
        FinanceAccount from = account(11L, new BigDecimal("60"));
        FinanceAccount to = account(22L, new BigDecimal("40"));
        to.setOwnerMemberId(ME);
        when(accountRepository.findById(11L)).thenReturn(Optional.of(from));
        when(accountRepository.findById(22L)).thenReturn(Optional.of(to));
        atomicBalanceStub();

        txService().delete(ME, 6L);

        // 两腿都删 + 双账户回滚
        verify(transactionRepository, times(2)).markDeleted(anyLong());
        assertEquals(0, from.getBalance().compareTo(new BigDecimal("100")));
        assertEquals(0, to.getBalance().compareTo(new BigDecimal("0")));
    }

    /* ===== 归属防线 ===== */

    @Test
    void foreignAccountThrows403() {
        FinanceAccount foreign = account(11L, new BigDecimal("100"));
        foreign.setOwnerMemberId(OTHER);
        when(accountRepository.findById(11L)).thenReturn(Optional.of(foreign));

        BizException e = assertThrows(BizException.class,
                () -> txService().create(ME, dto(2, 11L, "10")));
        assertEquals(403, e.getCode());
    }

    @Test
    void foreignTransactionDeleteThrows403() {
        FinanceTransaction foreign = new FinanceTransaction();
        foreign.setId(9L);
        foreign.setOwnerMemberId(OTHER);
        when(transactionRepository.findById(9L)).thenReturn(Optional.of(foreign));

        BizException e = assertThrows(BizException.class, () -> txService().delete(ME, 9L));
        assertEquals(403, e.getCode());
    }

    /* ===== 流水分页账期归一化（year/month 缺省规则在 service） ===== */

    @Test
    void listDefaultsToCurrentYearMonthWhenParamsAbsent() {
        when(transactionRepository.countByOwner(eq(ME), isNull(), isNull(),
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);

        txService().list(ME, null, null, null, null, 1, 20);

        ArgumentCaptor<LocalDateTime> start = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> end = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(transactionRepository).countByOwner(eq(ME), isNull(), isNull(), start.capture(), end.capture());
        YearMonth now = YearMonth.now();
        assertEquals(now.atDay(1).atStartOfDay(), start.getValue());
        assertEquals(now.plusMonths(1).atDay(1).atStartOfDay(), end.getValue());
    }

    @Test
    void listUsesExplicitYearMonthWhenProvided() {
        when(transactionRepository.countByOwner(eq(ME), isNull(), isNull(),
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);

        txService().list(ME, null, null, 2026, 1, 1, 20);

        ArgumentCaptor<LocalDateTime> start = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> end = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(transactionRepository).countByOwner(eq(ME), isNull(), isNull(), start.capture(), end.capture());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), start.getValue());
        assertEquals(LocalDateTime.of(2026, 2, 1, 0, 0), end.getValue());
    }

    /* ===== 建户 ===== */

    @Test
    void createAccountWithInitialBalanceRecordsAdjust() {
        java.util.concurrent.atomic.AtomicReference<FinanceAccount> inserted =
                new java.util.concurrent.atomic.AtomicReference<>();
        when(accountRepository.save(any(FinanceAccount.class))).thenAnswer(inv -> {
            inserted.set(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(accountRepository.findById(any(Long.class))).thenAnswer(inv -> Optional.ofNullable(inserted.get()));

        AccountSaveDTO dto = new AccountSaveDTO();
        dto.setName("公积金");
        dto.setAccountType("fund");
        dto.setInitialBalance(new BigDecimal("80000"));
        accountService().create(ME, dto);

        // 初始余额非 0 → 自动记一笔调整流水留痕
        ArgumentCaptor<FinanceTransaction> cap = ArgumentCaptor.forClass(FinanceTransaction.class);
        verify(transactionRepository).save(cap.capture());
        assertEquals(4, cap.getValue().getDirection());
        assertEquals(0, cap.getValue().getAmount().compareTo(new BigDecimal("80000")));
    }
}
