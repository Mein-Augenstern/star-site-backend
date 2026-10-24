package com.xp.starsite.finance.ai;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.finance.entity.FinanceAccount;
import com.xp.starsite.finance.ai.AiIntegration;
import com.xp.starsite.finance.ai.dto.FinanceDiagnosisDTO;
import com.xp.starsite.finance.ai.dto.FinanceDiagnosisRequestDTO;
import com.xp.starsite.finance.repository.FinanceAccountRepository;
import com.xp.starsite.finance.repository.FinanceTransactionRepository;
import com.xp.starsite.finance.service.ReportService;
import com.xp.starsite.finance.vo.BalanceSheetVO;
import com.xp.starsite.finance.vo.CashFlowVO;
import com.xp.starsite.finance.vo.IncomeExpenseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 财务健康诊断：聚合三张表 + 近 3 月收支 → 结构化快照发 AI 智能体服务
 * （财务顾问 prompt/模型在 AI 侧）→ 诊断建议文本。
 * 实时生成不落库（数据持续变化，缓存意义低）；
 * fail-open：archive 不可用时按 0 计入并在数据中标注；AI 不可用时抛 503（RpcException 翻译）。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceDiagnosisService {

    /** archive 不可用时的展示兜底文案 */
    private static final String ARCHIVE_UNAVAILABLE_TEXT = "未知";

    /** 现金流充裕时的展示文案 */
    private static final String MONTHS_COVERED_ADEQUATE = "充裕";

    private final FinanceAccountRepository accountRepository;
    private final FinanceTransactionRepository transactionRepository;
    private final ReportService reportService;
    private final AiIntegration aiIntegration;

    public String diagnose(Long memberId) {
        // 数据聚合（复用三张表口径）
        BalanceSheetVO sheet = reportService.balanceSheet(memberId);
        IncomeExpenseVO lastMonth = reportService.incomeExpense(memberId,
                LocalDateTime.now().minusMonths(1).getYear(),
                LocalDateTime.now().minusMonths(1).getMonthValue());
        CashFlowVO cash = reportService.cashFlow(memberId);
        List<FinanceAccount> accounts = accountRepository.listByOwner(memberId);

        FinanceDiagnosisDTO result;
        try {
            result = aiIntegration.diagnose(toRequest(sheet, lastMonth, cash, accounts));
        } catch (com.xp.starsite.common.rpc.RpcException e) {
            throw new BizException(503, "AI 服务暂不可用，请稍后再试");
        }
        return result.getText();
    }

    /** VO 聚合 → 结构化诊断请求（数值字符串原样透传，渲染在 AI 服务侧） */
    private FinanceDiagnosisRequestDTO toRequest(BalanceSheetVO sheet, IncomeExpenseVO lastMonth,
                                                 CashFlowVO cash, List<FinanceAccount> accountList) {
        FinanceDiagnosisRequestDTO request = new FinanceDiagnosisRequestDTO();

        FinanceDiagnosisRequestDTO.Sheet sheetDto = new FinanceDiagnosisRequestDTO.Sheet();
        sheetDto.setTotalAsset(text(sheet.getTotalAsset()));
        boolean archiveOk = Boolean.TRUE.equals(sheet.getArchiveAvailable());
        sheetDto.setArchiveAvailable(archiveOk);
        sheetDto.setArchiveAsset(archiveOk ? text(sheet.getArchiveAsset()) : ARCHIVE_UNAVAILABLE_TEXT);
        sheetDto.setTotalLiability(text(sheet.getTotalLiability()));
        sheetDto.setNetWorth(text(sheet.getNetWorth()));
        request.setSheet(sheetDto);

        FinanceDiagnosisRequestDTO.Month month = new FinanceDiagnosisRequestDTO.Month();
        month.setIncome(text(lastMonth.getIncome()));
        month.setExpense(text(lastMonth.getExpense()));
        month.setSurplus(text(lastMonth.getSurplus()));
        request.setLastMonth(month);

        FinanceDiagnosisRequestDTO.Cash cashDto = new FinanceDiagnosisRequestDTO.Cash();
        cashDto.setLiquidBalance(text(cash.getLiquidBalance()));
        cashDto.setMonthsCovered(cash.getMonthsCovered() == null ? MONTHS_COVERED_ADEQUATE : String.valueOf(cash.getMonthsCovered()));
        request.setCash(cashDto);

        List<FinanceDiagnosisRequestDTO.Account> accountDtos = new ArrayList<>();
        for (FinanceAccount a : accountList) {
            FinanceDiagnosisRequestDTO.Account account = new FinanceDiagnosisRequestDTO.Account();
            account.setName(a.getName());
            account.setBalance(text(a.getBalance()));
            accountDtos.add(account);
        }
        request.setAccounts(accountDtos);
        return request;
    }

    /** BigDecimal → 展示字符串（null → "-"） */
    private String text(java.math.BigDecimal value) {
        return value == null ? "-" : value.toPlainString();
    }
}
