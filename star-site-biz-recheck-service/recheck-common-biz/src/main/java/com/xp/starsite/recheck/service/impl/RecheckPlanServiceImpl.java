package com.xp.starsite.recheck.service.impl;

import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.api.dto.FamilyMemberSnapshotDTO;
import com.xp.starsite.api.dto.health.HealthIndicatorDTO;
import com.xp.starsite.api.dto.health.HealthReportDTO;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.common.util.IdGenerator;
import com.xp.starsite.recheck.assembler.ReminderRuleAssembler;
import com.xp.starsite.recheck.dto.RecheckPlanCloseDTO;
import com.xp.starsite.recheck.dto.RecheckPlanCreateDTO;
import com.xp.starsite.recheck.entity.RecheckPlan;
import com.xp.starsite.recheck.integration.HealthIntegration;
import com.xp.starsite.recheck.integration.ReminderIntegration;
import com.xp.starsite.recheck.repository.RecheckPlanRepository;
import com.xp.starsite.recheck.service.RecheckPlanService;
import com.xp.starsite.recheck.vo.RecheckPlanVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 复查闭环服务实现：编排 family（归属）/ health（报告只读）/ reminder（规则）三个横向服务。
 * RPC 参数组装经 {@link ReminderRuleAssembler}（core-service 层）完成后，
 * 由 {@link ReminderIntegration} 容错调用——提醒失败不回滚本地事务（增值能力非强一致）。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecheckPlanServiceImpl implements RecheckPlanService {

    /** 默认复查间隔（天）：与临床"3 个月后复查"惯例对齐 */
    private static final long DEFAULT_RECHECK_DAYS = 90;

    private final RecheckPlanRepository planRepository;
    private final FamilyOwnershipGuard ownershipGuard;
    private final HealthIntegration healthIntegration;
    private final ReminderIntegration reminderIntegration;
    private final IdGenerator idGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecheckPlanVO create(Long memberId, RecheckPlanCreateDTO dto) {
        FamilyMemberSnapshotDTO member = ownershipGuard.requireOwned(memberId, dto.getFamilyMemberId());
        HealthReportDTO report = healthIntegration.getReportDetail(memberId, dto.getReportId());
        if (!dto.getFamilyMemberId().equals(report.getFamilyMemberId())) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "报告不属于该家庭成员");
        }
        HealthIndicatorDTO indicator = report.getIndicators() == null ? null
                : report.getIndicators().stream()
                        .filter(i -> dto.getIndicatorId().equals(i.getId()))
                        .findFirst()
                        .orElse(null);
        if (indicator == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "该报告里找不到指定指标，请刷新后重试");
        }

        RecheckPlan plan = new RecheckPlan();
        plan.setId(idGenerator.nextId());
        plan.setFamilyId(member.getFamilyId());
        plan.setOwnerMemberId(memberId);
        plan.setFamilyMemberId(dto.getFamilyMemberId());
        plan.setSourceReportId(report.getId());
        plan.setSourceReportTitle(report.getTitle());
        plan.setSourceIndicatorId(indicator.getId());
        plan.setItemName(indicator.getItemName());
        plan.setBaselineValueNum(indicator.getValueNum());
        plan.setBaselineValueText(indicator.getValueText());
        plan.setUnit(indicator.getUnit());
        plan.setRefLow(indicator.getRefLow());
        plan.setRefHigh(indicator.getRefHigh());
        plan.setBaselineAbnormalFlag(indicator.getAbnormalFlag());
        plan.setTargetDate(dto.getTargetDate() != null ? dto.getTargetDate() : LocalDate.now().plusDays(DEFAULT_RECHECK_DAYS));
        plan.setMetricType(normalizeBlank(dto.getMetricType()));
        plan.setStatus(RecheckPlan.STATUS_ACTIVE);
        plan.setDeleted(0);
        planRepository.save(plan);

        // RPC 参数组装在 core-service 的 assembler；integration 只负责容错调用（失败不阻断主流程）
        reminderIntegration.upsert(ReminderRuleAssembler.reviewRule(plan, member.getName()));
        if (ReminderRuleAssembler.hasDailyMonitoring(plan)) {
            reminderIntegration.upsert(ReminderRuleAssembler.dailyRule(plan));
        }
        log.info("复查计划创建: planId={}, member={}, itemName={}, targetDate={}",
                plan.getId(), member.getName(), plan.getItemName(), plan.getTargetDate());
        return toVO(plan, member.getName());
    }

    @Override
    public List<RecheckPlanVO> list(Long memberId, Long familyMemberId, Integer status) {
        List<FamilyMemberSnapshotDTO> members = ownershipGuard.listOwnedMembers(memberId);
        Map<Long, String> nameById = members.stream()
                .collect(Collectors.toMap(FamilyMemberSnapshotDTO::getId, FamilyMemberSnapshotDTO::getName));
        if (familyMemberId != null && !nameById.containsKey(familyMemberId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权限访问该家庭成员的数据");
        }
        return planRepository.listByOwner(memberId, familyMemberId, status).stream()
                .map(plan -> toVO(plan, nameById.get(plan.getFamilyMemberId())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecheckPlanVO close(Long memberId, Long planId, RecheckPlanCloseDTO dto) {
        RecheckPlan plan = requireOwnedActivePlan(memberId, planId);
        HealthReportDTO report = healthIntegration.getReportDetail(memberId, dto.getReportId());
        if (!plan.getFamilyMemberId().equals(report.getFamilyMemberId())) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "复查报告不属于该计划的成员");
        }

        HealthIndicatorDTO latest = report.indicatorByName(plan.getItemName());
        plan.setClosedReportId(report.getId());
        plan.setClosedReportTitle(report.getTitle());
        plan.setConclusion(buildConclusion(plan, latest));
        plan.setStatus(RecheckPlan.STATUS_CLOSED);
        planRepository.update(plan);

        cancelRulesTolerant(plan);
        log.info("复查计划闭环: planId={}, itemName={}, conclusion={}", plan.getId(), plan.getItemName(), plan.getConclusion());
        return toVO(plan, memberName(memberId, plan.getFamilyMemberId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long memberId, Long planId) {
        RecheckPlan plan = requireOwnedActivePlan(memberId, planId);
        plan.setStatus(RecheckPlan.STATUS_CANCELLED);
        planRepository.update(plan);
        cancelRulesTolerant(plan);
        log.info("复查计划取消: planId={}", plan.getId());
    }

    /** 撤销该计划的全部提醒规则（复查日一次性 + 可选每日监测，tolerant 不阻断主流程） */
    private void cancelRulesTolerant(RecheckPlan plan) {
        reminderIntegration.delete(ReminderRuleAssembler.deleteKey(plan, ReminderRuleAssembler.BIZ_TYPE_REVIEW));
        if (ReminderRuleAssembler.hasDailyMonitoring(plan)) {
            reminderIntegration.delete(ReminderRuleAssembler.deleteKey(plan, ReminderRuleAssembler.BIZ_TYPE_DAILY));
        }
    }

    /** 归属防线：计划必须存在且归属当前账号（fail-closed），且仍处于进行中 */
    private RecheckPlan requireOwnedActivePlan(Long memberId, Long planId) {
        RecheckPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new BizException(ResultCode.BAD_REQUEST.getCode(), "复查计划不存在"));
        // 全部计划行的 owner_member_id 都在创建时固化为调用方账号，等值校验即可收口归属
        if (!plan.getOwnerMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权限操作该复查计划");
        }
        if (plan.getStatus() != RecheckPlan.STATUS_ACTIVE) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "复查计划已完结或已取消");
        }
        return plan;
    }

    /** 闭环结论：复查报告中按指标名对齐基线，异常标志优先、数值趋势兜底 */
    private String buildConclusion(RecheckPlan plan, HealthIndicatorDTO latest) {
        if (latest == null) {
            return "复查报告中未见「" + plan.getItemName() + "」项";
        }
        StringBuilder conclusion = new StringBuilder(plan.getItemName()).append("：").append(valueText(latest));
        String range = HealthReportDTO.refRangeText(latest.getRefLow(), latest.getRefHigh());
        if (!range.isEmpty()) {
            conclusion.append("（参考区间 ").append(range).append("）");
        }
        if (latest.getAbnormalFlag() != null && latest.getAbnormalFlag() == 0) {
            return conclusion.append("，已回到正常范围").toString();
        }
        if (latest.getValueNum() != null && plan.getBaselineValueNum() != null) {
            int cmp = latest.getValueNum().compareTo(plan.getBaselineValueNum());
            if (cmp == 0) {
                return conclusion.append("，与基线持平").toString();
            }
            String delta = latest.getValueNum().subtract(plan.getBaselineValueNum()).abs()
                    .stripTrailingZeros().toPlainString();
            return conclusion.append(cmp < 0 ? "，较基线下降 " : "，较基线上升 ").append(delta).toString();
        }
        return conclusion.toString();
    }

    private String valueText(HealthIndicatorDTO indicator) {
        if (indicator.getValueText() != null && !indicator.getValueText().isBlank()) {
            return indicator.getValueText();
        }
        String num = indicator.getValueNum() == null ? "-" : indicator.getValueNum().stripTrailingZeros().toPlainString();
        return indicator.getUnit() == null ? num : num + indicator.getUnit();
    }

    private String memberName(Long memberId, Long familyMemberId) {
        return ownershipGuard.listOwnedMembers(memberId).stream()
                .filter(m -> familyMemberId.equals(m.getId()))
                .map(FamilyMemberSnapshotDTO::getName)
                .findFirst()
                .orElse(null);
    }

    private RecheckPlanVO toVO(RecheckPlan plan, String memberName) {
        RecheckPlanVO vo = new RecheckPlanVO();
        vo.setId(plan.getId());
        vo.setFamilyMemberId(plan.getFamilyMemberId());
        vo.setMemberName(memberName);
        vo.setItemName(plan.getItemName());
        vo.setBaselineText(plan.getBaselineValueText() != null
                ? plan.getBaselineValueText()
                : (plan.getBaselineValueNum() == null ? null : plan.getBaselineValueNum().stripTrailingZeros().toPlainString()));
        vo.setRefRange(HealthReportDTO.refRangeText(plan.getRefLow(), plan.getRefHigh()));
        vo.setUnit(plan.getUnit());
        vo.setTargetDate(plan.getTargetDate());
        vo.setDaysLeft(plan.getTargetDate() == null ? null : ChronoUnit.DAYS.between(LocalDate.now(), plan.getTargetDate()));
        vo.setStatus(plan.getStatus());
        vo.setStatusText(switch (plan.getStatus()) {
            case RecheckPlan.STATUS_ACTIVE -> "进行中";
            case RecheckPlan.STATUS_CLOSED -> "已闭环";
            case RecheckPlan.STATUS_CANCELLED -> "已取消";
            default -> "未知";
        });
        vo.setMetricType(plan.getMetricType());
        vo.setSourceReportId(plan.getSourceReportId());
        vo.setSourceReportTitle(plan.getSourceReportTitle());
        vo.setClosedReportId(plan.getClosedReportId());
        vo.setClosedReportTitle(plan.getClosedReportTitle());
        vo.setConclusion(plan.getConclusion());
        vo.setCreatedAt(plan.getCreatedAt());
        return vo;
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
