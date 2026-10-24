package com.xp.starsite.finance;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.rpc.RpcException;
import com.xp.starsite.finance.ai.FinanceDiagnosisService;
import com.xp.starsite.finance.ai.AiIntegration;
import com.xp.starsite.finance.ai.dto.FinanceDiagnosisDTO;
import com.xp.starsite.finance.ai.dto.FinanceDiagnosisRequestDTO;
import com.xp.starsite.finance.repository.FinanceAccountRepository;
import com.xp.starsite.finance.repository.FinanceTransactionRepository;
import com.xp.starsite.finance.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 财务 AI 诊断单元测试：聚合快照结构化上送 AI 服务、失败翻译 503（mock 全部依赖）。
 * LLM 拆分到 ai 服务后，本测试只验证「聚合 → 集成层」的编排与容错（prompt 在 AI 服务侧）。
 *
 * @author muyi
 * @since 2026-10
 */
@ExtendWith(MockitoExtension.class)
class FinanceDiagnosisTest {

    private static final long ME = 1L;

    @Mock
    private FinanceAccountRepository accountRepository;
    @Mock
    private FinanceTransactionRepository transactionRepository;
    @Mock
    private ReportService reportService;
    @Mock
    private AiIntegration aiIntegration;

    private FinanceDiagnosisService service() {
        return new FinanceDiagnosisService(accountRepository, transactionRepository, reportService, aiIntegration);
    }

    private void stubReports() {
        when(reportService.balanceSheet(ME)).thenReturn(sheet("2105000", "8000"));
        when(reportService.incomeExpense(ME, 2026, 9)).thenReturn(ie("12000", "4000"));
        when(reportService.cashFlow(ME)).thenReturn(new com.xp.starsite.finance.vo.CashFlowVO());
        when(accountRepository.listByOwner(ME)).thenReturn(List.of());
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
    void diagnosisSendsStructuredSnapshot() {
        stubReports();
        when(aiIntegration.diagnose(any(FinanceDiagnosisRequestDTO.class)))
                .thenReturn(new FinanceDiagnosisDTO());

        service().diagnose(ME);

        ArgumentCaptor<FinanceDiagnosisRequestDTO> captor =
                ArgumentCaptor.forClass(FinanceDiagnosisRequestDTO.class);
        org.mockito.Mockito.verify(aiIntegration).diagnose(captor.capture());
        var req = captor.getValue();
        // 数据结构化进了请求（不再是拼 Prompt 字符串）
        assertEquals("2105000", req.getSheet().getTotalAsset());
        assertEquals("2097000", req.getSheet().getNetWorth());
        assertEquals("8000", req.getLastMonth().getSurplus());
    }

    @Test
    void aiFailureTranslatedTo503() {
        stubReports();
        when(aiIntegration.diagnose(any(FinanceDiagnosisRequestDTO.class)))
                .thenThrow(new RpcException(500, "依赖服务暂不可用，请稍后再试", true));

        BizException e = assertThrows(BizException.class, () -> service().diagnose(ME));
        assertEquals(503, e.getCode());
    }
}
