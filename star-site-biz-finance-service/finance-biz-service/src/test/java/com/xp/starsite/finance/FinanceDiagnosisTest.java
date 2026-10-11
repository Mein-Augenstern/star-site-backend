package com.xp.starsite.finance;

import com.xp.starsite.api.client.ArchiveApi;
import com.xp.starsite.api.dto.AssetSummaryDTO;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.llm.LlmClient;
import com.xp.starsite.common.result.R;
import com.xp.starsite.finance.ai.FinanceDiagnosisService;
import com.xp.starsite.finance.mapper.FinanceAccountMapper;
import com.xp.starsite.finance.mapper.FinanceTransactionMapper;
import com.xp.starsite.finance.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 财务 AI 诊断单元测试：数据聚合进 Prompt、未配 key 降级、LLM 失败翻译（mock 全部依赖）
 *
 * @author xiaopu
 * @since 2026-10
 */
@ExtendWith(MockitoExtension.class)
class FinanceDiagnosisTest {

    private static final long ME = 1L;

    @Mock
    private FinanceAccountMapper accountMapper;
    @Mock
    private FinanceTransactionMapper transactionMapper;
    @Mock
    private ReportService reportService;
    @Mock
    private ArchiveApi archiveApi;
    @Mock
    private LlmClient llmClient;
    @Mock
    private ObjectProvider<LlmClient> llmProvider;

    @org.junit.jupiter.api.BeforeEach
    void lenientStubs() {
        // archive stub 在无 key 用例中不被调用，放宽严格模式
        org.mockito.Mockito.lenient().when(archiveApi.assetSummary(ME))
                .thenReturn(R.ok(new AssetSummaryDTO(BigDecimal.ZERO)));
    }

    private FinanceDiagnosisService service() {
        when(llmProvider.getIfAvailable()).thenReturn(llmClient);
        return new FinanceDiagnosisService(accountMapper, transactionMapper, reportService, archiveApi, llmProvider);
    }

    private FinanceDiagnosisService serviceWithoutLlm() {
        when(llmProvider.getIfAvailable()).thenReturn(null);
        return new FinanceDiagnosisService(accountMapper, transactionMapper, reportService, archiveApi, llmProvider);
    }

    private void stubReports() {
        when(reportService.balanceSheet(ME)).thenReturn(sheet("2105000", "8000"));
        when(reportService.incomeExpense(ME, 2026, 9)).thenReturn(ie("12000", "4000"));
        when(reportService.cashFlow(ME)).thenReturn(new com.xp.starsite.finance.vo.CashFlowVO());
        when(accountMapper.selectList(any())).thenReturn(List.of());
    }

    private com.xp.starsite.finance.vo.BalanceSheetVO sheet(String asset, String liability) {
        var vo = new com.xp.starsite.finance.vo.BalanceSheetVO();
        vo.setAccountAsset(new BigDecimal("105000"));
        vo.setArchiveAsset(new BigDecimal(asset));
        vo.setArchiveAvailable(true);
        vo.setTotalAsset(new BigDecimal("2105000"));
        vo.setTotalLiability(new BigDecimal(liability));
        vo.setNetWorth(new BigDecimal("2097000"));
        return vo;
    }

    private com.xp.starsite.finance.vo.IncomeExpenseVO ie(String income, String expense) {
        var vo = new com.xp.starsite.finance.vo.IncomeExpenseVO();
        vo.setIncome(new BigDecimal(income));
        vo.setExpense(new BigDecimal(expense));
        vo.setSurplus(new BigDecimal("8000"));
        return vo;
    }

    @Test
    void diagnosisCallsLlmWithData() {
        stubReports();
        when(llmClient.chat(anyString(), anyString())).thenReturn("诊断结论……");

        String text = service().diagnose(ME);

        assertEquals("诊断结论……", text);
        var sysCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        var userCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(llmClient).chat(sysCaptor.capture(), userCaptor.capture());
        assertTrue(sysCaptor.getValue().contains("家庭财务顾问"));
        // 数据进了 Prompt
        assertTrue(userCaptor.getValue().contains("2105000"));
        assertTrue(userCaptor.getValue().contains("结余：8000"));
    }

    @Test
    void noKeyMeansDisabled() {
        when(llmProvider.getIfAvailable()).thenReturn(null);

        BizException e = assertThrows(BizException.class, () -> serviceWithoutLlm().diagnose(ME));
        assertEquals(400, e.getCode());
    }

    @Test
    void llmFailureTranslatedTo503() {
        stubReports();
        when(llmClient.chat(anyString(), anyString()))
                .thenThrow(new LlmClient.LlmException("timeout", null));

        BizException e = assertThrows(BizException.class, () -> service().diagnose(ME));
        assertEquals(503, e.getCode());
    }

    private static void assertTrue(boolean cond) {
        org.junit.jupiter.api.Assertions.assertTrue(cond);
    }
}
