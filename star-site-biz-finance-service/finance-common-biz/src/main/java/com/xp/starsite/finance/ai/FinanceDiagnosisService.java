package com.xp.starsite.finance.ai;

import com.xp.starsite.common.llm.LlmClient;
import com.xp.starsite.finance.entity.FinanceAccount;
import com.xp.starsite.finance.repository.FinanceAccountRepository;
import com.xp.starsite.finance.repository.FinanceTransactionRepository;
import com.xp.starsite.finance.service.ReportService;
import com.xp.starsite.finance.vo.BalanceSheetVO;
import com.xp.starsite.finance.vo.CashFlowVO;
import com.xp.starsite.finance.vo.IncomeExpenseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 财务健康诊断：聚合三张表 + 近 3 月收支 → LLM 生成诊断建议。
 * 实时生成不落库（数据持续变化，缓存意义低）；LLM 不可用时抛 400 提示未启用 / 503 不可用。
 * fail-open：archive 不可用时按 0 计入并在数据中标注。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceDiagnosisService {

    private static final String SYSTEM_PROMPT = """
            你是家庭财务顾问。请基于给出的家庭财务数据做健康诊断：
            1. 用三句话概括资产/收支/现金流的现状；
            2. 指出 2-3 个最值得注意的问题或风险（如月供占比过高、应急资金不足、结余率低）；
            3. 给出可执行的改善建议；
            4. 语气务实温和，不推销任何金融产品，最后声明仅供参考。
            全文 350 字以内，不要使用 Markdown 标记。""";

    private final FinanceAccountRepository accountRepository;
    private final FinanceTransactionRepository transactionRepository;
    private final ReportService reportService;
    private final ObjectProvider<LlmClient> llmClient;

    public String diagnose(Long memberId) {
        LlmClient client = llmClient.getIfAvailable();
        if (client == null) {
            throw new com.xp.starsite.common.exception.BizException(400, "AI 诊断未启用（未配置 LLM_API_KEY）");
        }

        // 数据聚合（复用三张表口径）
        BalanceSheetVO sheet = reportService.balanceSheet(memberId);
        IncomeExpenseVO lastMonth = reportService.incomeExpense(memberId,
                LocalDateTime.now().minusMonths(1).getYear(),
                LocalDateTime.now().minusMonths(1).getMonthValue());
        CashFlowVO cash = reportService.cashFlow(memberId);
        List<FinanceAccount> accounts = accountRepository.listByOwner(memberId);

        StringBuilder user = new StringBuilder("家庭财务数据：\n");
        user.append("- 总资产：").append(sheet.getTotalAsset()).append("（其中档案资产估值 ")
                .append(Boolean.TRUE.equals(sheet.getArchiveAvailable()) ? sheet.getArchiveAsset() : "未知").append("）\n");
        user.append("- 总负债：").append(sheet.getTotalLiability()).append("\n");
        user.append("- 净资产：").append(sheet.getNetWorth()).append("\n");
        user.append("- 上月收入：").append(lastMonth.getIncome()).append("，支出：").append(lastMonth.getExpense())
                .append("，结余：").append(lastMonth.getSurplus()).append("\n");
        user.append("- 流动资金：").append(cash.getLiquidBalance())
                .append("，可撑月数：").append(cash.getMonthsCovered() == null ? "充裕" : cash.getMonthsCovered()).append("\n");
        user.append("- 账户清单：");
        for (FinanceAccount a : accounts) {
            user.append(a.getName()).append("(").append(a.getBalance()).append(") ");
        }
        user.append("\n");

        String text;
        try {
            text = client.chat(SYSTEM_PROMPT, user.toString());
        } catch (LlmClient.LlmException e) {
            throw new com.xp.starsite.common.exception.BizException(503, "AI 服务暂不可用，请稍后再试");
        }
        log.info("财务诊断完成: owner={}, len={}", memberId, text.length());
        return text;
    }
}
