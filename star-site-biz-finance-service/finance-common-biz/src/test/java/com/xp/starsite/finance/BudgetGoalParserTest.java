package com.xp.starsite.finance;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.util.IdGenerator;
import com.xp.starsite.finance.dto.BudgetSaveDTO;
import com.xp.starsite.finance.dto.GoalSaveDTO;
import com.xp.starsite.finance.entity.FinanceBudget;
import com.xp.starsite.finance.entity.FinanceGoal;
import com.xp.starsite.finance.entity.FinanceTransaction;
import com.xp.starsite.finance.importer.BillParseResult;
import com.xp.starsite.finance.importer.BillParserRegistry;
import com.xp.starsite.finance.importer.WechatBillParser;
import com.xp.starsite.finance.repository.FinanceBudgetRepository;
import com.xp.starsite.finance.repository.FinanceGoalRepository;
import com.xp.starsite.finance.repository.FinanceTransactionRepository;
import com.xp.starsite.finance.service.impl.BudgetServiceImpl;
import com.xp.starsite.finance.service.impl.GoalServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * 预算/目标/账单解析单元测试（mock 全部依赖；解析器用 mock 微信账单样本）
 *
 * @author xiaopu
 * @since 2026-10
 */
@ExtendWith(MockitoExtension.class)
class BudgetGoalParserTest {

    private static final long ME = 1L;

    @Mock
    private FinanceBudgetRepository budgetRepository;
    @Mock
    private FinanceGoalRepository goalRepository;
    @Mock
    private FinanceTransactionRepository transactionRepository;
    @Mock
    private IdGenerator idGenerator;

    /* ===== 预算 ===== */

    @Test
    void budgetUsageRateReflectsSpent() {
        // 当月 FOOD 支出 60，预算 100 → 使用率 0.6，剩余 40
        when(transactionRepository.listByDirectionAndPeriod(any(), anyInt(), any(), any(), any()))
                .thenReturn(List.of(txExpense("60"), txExpense("30")));
        when(budgetRepository.findByOwnerCategoryPeriod(any(), any(), any())).thenReturn(Optional.empty());

        BudgetSaveDTO dto = new BudgetSaveDTO();
        dto.setCategory("FOOD");
        dto.setPeriod("2026-10");
        dto.setAmount(new BigDecimal("100"));
        com.xp.starsite.finance.vo.BudgetVO vo =
                new BudgetServiceImpl(budgetRepository, transactionRepository, idGenerator).save(ME, dto);

        assertEquals(0, vo.getSpent().compareTo(new BigDecimal("90")));
        assertEquals(0, vo.getRemaining().compareTo(new BigDecimal("10")));
        assertEquals(0, vo.getUsageRate().compareTo(new BigDecimal("0.9000")));
    }

    @Test
    void budgetOverspendAllowsNegativeRemaining() {
        when(transactionRepository.listByDirectionAndPeriod(any(), anyInt(), any(), any(), any()))
                .thenReturn(List.of(txExpense("150")));
        FinanceBudget exist = new FinanceBudget();
        exist.setId(7L);
        exist.setOwnerMemberId(ME);
        exist.setCategory("FOOD");
        exist.setPeriod("2026-10");
        exist.setAmount(new BigDecimal("100"));
        when(budgetRepository.findByOwnerCategoryPeriod(any(), any(), any())).thenReturn(Optional.of(exist));

        BudgetSaveDTO dto = new BudgetSaveDTO();
        dto.setCategory("FOOD");
        dto.setPeriod("2026-10");
        dto.setAmount(new BigDecimal("100"));
        com.xp.starsite.finance.vo.BudgetVO vo =
                new BudgetServiceImpl(budgetRepository, transactionRepository, idGenerator).save(ME, dto);

        // 超支：remaining 为负、usageRate > 1
        assertEquals(0, vo.getRemaining().compareTo(new BigDecimal("-50")));
        assertEquals(0, vo.getUsageRate().compareTo(new BigDecimal("1.5000")));
    }

    @Test
    void budgetRejectsInvalidCategory() {
        BudgetSaveDTO dto = new BudgetSaveDTO();
        dto.setCategory("NOT_EXIST");
        dto.setPeriod("2026-10");
        dto.setAmount(new BigDecimal("100"));
        assertThrows(BizException.class,
                () -> new BudgetServiceImpl(budgetRepository, transactionRepository, idGenerator).save(ME, dto));
    }

    /* ===== 目标 ===== */

    @Test
    void goalProgressComputed() {
        when(goalRepository.save(any(FinanceGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        GoalSaveDTO dto = new GoalSaveDTO();
        dto.setName("教育金");
        dto.setTargetAmount(new BigDecimal("200000"));
        dto.setCurrentAmount(new BigDecimal("50000"));
        com.xp.starsite.finance.vo.GoalVO vo =
                new GoalServiceImpl(goalRepository, idGenerator).create(ME, dto);

        assertEquals(0, vo.getProgress().compareTo(new BigDecimal("0.2500")));
    }

    @Test
    void foreignGoalUpdateThrows403() {
        FinanceGoal foreign = new FinanceGoal();
        foreign.setId(3L);
        foreign.setOwnerMemberId(OTHER);
        when(goalRepository.findById(3L)).thenReturn(Optional.of(foreign));

        GoalSaveDTO dto = new GoalSaveDTO();
        dto.setName("x");
        dto.setTargetAmount(new BigDecimal("1"));
        BizException e = assertThrows(BizException.class,
                () -> new GoalServiceImpl(goalRepository, idGenerator).update(ME, 3L, dto));
        assertEquals(403, e.getCode());
    }

    /* ===== 账单解析（微信 mock 样本） ===== */

    private String wechatSample() throws IOException {
        return Files.readString(
                new ClassPathResource("bills/wechat-bill-sample.csv").getFile().toPath(),
                StandardCharsets.UTF_8);
    }

    @Test
    void wechatBillParsed() throws IOException {
        String content = wechatSample();
        WechatBillParser parser = new WechatBillParser();
        assertTrue(parser.supports(content), "格式探测应命中微信");

        BillParseResult result = parser.parse(content, 88L, java.util.Map.of());

        // 4 行明细：3 支出 + 1 收入；中性/退款 0
        assertEquals(4, result.getTransactions().size());
        assertEquals(0, result.getSkipped());
        FinanceTransaction first = result.getTransactions().get(0);
        assertEquals(2, first.getDirection());
        assertEquals(0, first.getAmount().compareTo(new BigDecimal("28.50")));
        assertEquals(1000120261003202501L + "", first.getExternalId());
        assertEquals(88L, first.getAccountId());
        // 收入行方向
        assertEquals(1, result.getTransactions().get(1).getDirection());
    }

    @Test
    void registryDetectsChannel() throws IOException {
        BillParserRegistry registry = new BillParserRegistry(List.of(new WechatBillParser()));
        assertEquals("wechat", registry.detect(wechatSample()).channel());
        assertThrows(IllegalArgumentException.class, () -> registry.detect("随便一段文本"));
    }

    /* ===== 支付宝样本 ===== */

    @Test
    void alipayBillSkipsRefundRows() throws IOException {
        String content = Files.readString(
                new ClassPathResource("bills/alipay-bill-sample.csv").getFile().toPath(),
                StandardCharsets.UTF_8);
        com.xp.starsite.finance.importer.AlipayBillParser parser =
                new com.xp.starsite.finance.importer.AlipayBillParser();
        assertTrue(parser.supports(content));

        BillParseResult result = parser.parse(content, 88L, java.util.Map.of());
        // 3 行明细，1 行"不计收支"（退款）跳过 → 2 笔入账
        assertEquals(2, result.getTransactions().size());
        assertEquals(1, result.getSkipped());
        assertEquals(3, result.getTransactions().get(0).getSource());
    }

    private FinanceTransaction txExpense(String amount) {
        FinanceTransaction t = new FinanceTransaction();
        t.setOwnerMemberId(ME);
        t.setDirection(2);
        t.setCategory("FOOD");
        t.setAmount(new BigDecimal(amount));
        t.setTransTime(LocalDateTime.now());
        return t;
    }

    private static final long OTHER = 2L;
}
