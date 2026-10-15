package com.xp.starsite.finance.controller;

import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.finance.controller.actions.FinanceActions;
import com.xp.starsite.finance.service.ReportService;
import com.xp.starsite.finance.vo.BalanceSheetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务间内部接口（Feign/智能体工具提供方）：资产负债表只读查询。
 * <p>
 * /internal/** 不在网关路由表里，外网请求到网关直接 404；
 * JwtAuthInterceptor 只拦 /api/**，本控制器不经 JWT，仅限内网服务间调用
 * （AI 智能体的 finance 工具按 memberId 透传调用，fail-closed）。
 * 业务逻辑统一经模板包裹（动作名 finance.internal.*）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/internal/finance")
@RequiredArgsConstructor
public class FinanceInternalController {

    private final ReportService reportService;
    private final QueryTemplate queryTemplate;

    /**
     * 资产负债表（三张表口径，读场景 fail-open：archive 不可用时档案资产按 0 计并标注）
     *
     * @param memberId 归属账号 id
     * @return 资产负债表视图
     */
    @GetMapping("/balance-sheet")
    public R<BalanceSheetVO> balanceSheet(@RequestParam("memberId") Long memberId) {
        return queryTemplate.query(FinanceActions.INTERNAL_BALANCE_SHEET,
                () -> reportService.balanceSheet(memberId));
    }
}
