package com.xp.starsite.recheck.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.recheck.controller.actions.RecheckActions;
import com.xp.starsite.recheck.dto.RecheckPlanCloseDTO;
import com.xp.starsite.recheck.dto.RecheckPlanCreateDTO;
import com.xp.starsite.recheck.service.RecheckPlanService;
import com.xp.starsite.recheck.vo.RecheckPlanVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 复查闭环接口：异常指标 → 复查计划 → 提醒/每日监测 → 新报告对比闭环。
 * 鉴权：JwtAuthInterceptor 拦 /api/**，memberId 从网关注入的 JWT attribute 取；
 * 业务逻辑统一经模板包裹（动作名 recheck.plan.*）。
 *
 * @author muyi
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/recheck")
@RequiredArgsConstructor
public class RecheckController {

    private final RecheckPlanService planService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 从报告指标创建复查计划（同时注册复查日提醒 + 可选每日监测规则）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param dto      创建内容
     * @return 计划视图
     */
    @PostMapping("/plans")
    public R<RecheckPlanVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                   @Valid @RequestBody RecheckPlanCreateDTO dto) {
        return writeTemplate.write(RecheckActions.PLAN_CREATE,
                () -> planService.create(memberId, dto));
    }

    /**
     * 计划列表（全家范围或指定成员）
     *
     * @param memberId       当前登录账号（JWT 注入）
     * @param familyMemberId 可选，指定成员则单查
     * @param status         可选，按状态过滤（1进行中 2已闭环 3已取消）
     * @return 计划视图列表
     */
    @GetMapping("/plans")
    public R<List<RecheckPlanVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                       @RequestParam(value = "familyMemberId", required = false) Long familyMemberId,
                                       @RequestParam(value = "status", required = false) Integer status) {
        return queryTemplate.query(RecheckActions.PLAN_LIST,
                () -> planService.list(memberId, familyMemberId, status));
    }

    /**
     * 闭环：关联复查新报告，按指标名对齐基线生成对比结论
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param planId   计划 id
     * @param dto      复查新报告
     * @return 闭环后的计划视图（含结论）
     */
    @PostMapping("/plans/{planId}/close")
    public R<RecheckPlanVO> close(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                  @PathVariable("planId") Long planId,
                                  @Valid @RequestBody RecheckPlanCloseDTO dto) {
        return writeTemplate.write(RecheckActions.PLAN_CLOSE,
                () -> planService.close(memberId, planId, dto));
    }

    /**
     * 取消计划（撤销提醒/监测规则）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param planId   计划 id
     */
    @PostMapping("/plans/{planId}/cancel")
    public R<Void> cancel(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable("planId") Long planId) {
        return writeTemplate.write(RecheckActions.PLAN_CANCEL,
                () -> planService.cancel(memberId, planId));
    }
}
