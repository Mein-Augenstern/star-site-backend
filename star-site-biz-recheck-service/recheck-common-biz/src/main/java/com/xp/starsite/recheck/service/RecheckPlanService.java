package com.xp.starsite.recheck.service;

import com.xp.starsite.recheck.dto.RecheckPlanCloseDTO;
import com.xp.starsite.recheck.dto.RecheckPlanCreateDTO;
import com.xp.starsite.recheck.vo.RecheckPlanVO;

import java.util.List;

/**
 * 复查闭环服务：异常指标 → 复查计划（+提醒/每日监测规则）→ 复查新报告对比闭环。
 * <p>
 * 归属防线约定：所有方法首参 memberId 为当前登录顶梁柱（Controller 从 JWT attribute 取），
 * 带 familyMemberId 的操作先经 {@link com.xp.starsite.api.client.FamilyOwnershipGuard} 校验；
 * 报告/指标数据经 health-facade 从 health（横向服务）只读获取，本服务不落报告数据。
 *
 * @author muyi
 * @since 2026-10
 */
public interface RecheckPlanService {

    /**
     * 从报告指标创建复查计划（同时注册复查日提醒 + 可选每日监测规则）
     *
     * @param memberId 当前登录账号
     * @param dto      创建内容（成员/报告/指标必填）
     * @return 计划视图
     * @throws com.xp.starsite.common.exception.BizException 403 成员/报告不归属；400 报告无此指标
     */
    RecheckPlanVO create(Long memberId, RecheckPlanCreateDTO dto);

    /**
     * 计划列表（全家范围或指定成员）
     *
     * @param memberId       当前登录账号
     * @param familyMemberId 可选，指定成员则单查（校验归属）；不传=全家范围
     * @param status         可选，按状态过滤（1进行中 2已闭环 3已取消）
     * @return 计划视图列表（按创建时间倒序）
     * @throws com.xp.starsite.common.exception.BizException 403 家庭成员不归属当前账号
     */
    List<RecheckPlanVO> list(Long memberId, Long familyMemberId, Integer status);

    /**
     * 闭环：关联复查新报告，按指标名对齐基线生成对比结论，完结计划并撤销规则
     *
     * @param memberId 当前登录账号
     * @param planId   计划 id
     * @param dto      复查新报告
     * @return 闭环后的计划视图（含结论）
     * @throws com.xp.starsite.common.exception.BizException 400 计划不存在/已完结；403 报告不归属
     */
    RecheckPlanVO close(Long memberId, Long planId, RecheckPlanCloseDTO dto);

    /**
     * 取消计划（撤销提醒/监测规则）
     *
     * @param memberId 当前登录账号
     * @param planId   计划 id
     * @throws com.xp.starsite.common.exception.BizException 400 计划不存在/已完结
     */
    void cancel(Long memberId, Long planId);
}
