package com.xp.starsite.insurance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.api.dto.FamilyMemberSnapshotDTO;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.mq.EventPublisher;
import com.xp.starsite.common.mq.event.EventTypes;
import com.xp.starsite.common.mq.event.Topics;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.insurance.dto.PolicySaveDTO;
import com.xp.starsite.insurance.entity.InsurancePolicy;
import com.xp.starsite.insurance.mapper.InsurancePolicyMapper;
import com.xp.starsite.insurance.service.InsuranceService;
import com.xp.starsite.insurance.vo.CoverageMapVO;
import com.xp.starsite.insurance.vo.PageVO;
import com.xp.starsite.insurance.vo.PolicyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 保单服务实现。
 * <p>
 * 提醒事件与业务落库的一致性取舍：
 * - 缴费提醒（主提醒）走事务消息，「落库成功 ⇔ 事件一定发出」；
 * - 到期提醒 / 删除撤销走普通发送，丢失只少一条提醒、不产生脏数据，不值得为它扩事务范围。
 * 消费端（reminder-service，Step 2）以 (source, bizType, bizId) 幂等。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsuranceServiceImpl implements InsuranceService {

    private static final Set<String> CATEGORIES = Set.of(
            "critical_illness", "major_medical", "accident", "life",
            "annuity", "vehicle", "property", "other");

    /** 险种固定顺序（保障地图矩阵列序） */
    private static final List<String> CATEGORY_ORDER = List.of(
            "critical_illness", "major_medical", "accident", "life",
            "annuity", "vehicle", "property", "other");

    private final InsurancePolicyMapper policyMapper;
    private final FamilyOwnershipGuard ownershipGuard;
    private final EventPublisher eventPublisher;

    @Override
    public PolicyVO create(Long memberId, PolicySaveDTO dto) {
        FamilyMemberSnapshotDTO insured = ownershipGuard.requireOwned(memberId, dto.getInsuredMemberId());
        InsurancePolicy policy = new InsurancePolicy();
        // 雪花 id 应用侧先赋值：半消息的载荷里就要带 bizId，insert 发生在事务回调里
        policy.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
        applyDto(policy, dto);
        policy.setFamilyId(insured.getFamilyId());
        policy.setOwnerMemberId(memberId);
        policy.setInsuredMemberName(insured.getName());
        // 主提醒与落库原子：半消息 → insert → 提交；insert 失败消息回滚
        eventPublisher.publishTransactional(Topics.TOPIC_REMINDER, EventTypes.REMINDER_REGISTER,
                "policy", policy.getId(), paymentReminderOf(policy), () -> policyMapper.insert(policy));
        publishExpiryReminder(policy);
        log.info("创建保单: id={}, insured={}, category={}", policy.getId(), insured.getName(), policy.getCategory());
        return toVO(policyMapper.selectById(policy.getId()));
    }

    @Override
    public PageVO<PolicyVO> list(Long memberId, Long insuredMemberId, String category, Integer status,
                                 long page, long size) {
        if (insuredMemberId != null) {
            ownershipGuard.requireOwned(memberId, insuredMemberId);
        }
        LambdaQueryWrapper<InsurancePolicy> wrapper = new LambdaQueryWrapper<InsurancePolicy>()
                .eq(InsurancePolicy::getOwnerMemberId, memberId)
                .eq(insuredMemberId != null, InsurancePolicy::getInsuredMemberId, insuredMemberId)
                .eq(StringUtils.hasText(category), InsurancePolicy::getCategory, category)
                .eq(status != null, InsurancePolicy::getStatus, status)
                .orderByDesc(InsurancePolicy::getId);
        Page<InsurancePolicy> result = policyMapper.selectPage(Page.of(page, size), wrapper);
        return PageVO.of(result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PolicyVO getDetail(Long memberId, Long id) {
        return toVO(requireOwnedPolicy(memberId, id));
    }

    @Override
    public PolicyVO update(Long memberId, Long id, PolicySaveDTO dto) {
        InsurancePolicy policy = requireOwnedPolicy(memberId, id);
        FamilyMemberSnapshotDTO insured = ownershipGuard.requireOwned(memberId, dto.getInsuredMemberId());
        applyDto(policy, dto);
        policy.setInsuredMemberName(insured.getName());
        policyMapper.updateById(policy);
        // Register 是 upsert 语义：被保人/日期改了，重发覆盖规则
        eventPublisher.publish(Topics.TOPIC_REMINDER, EventTypes.REMINDER_REGISTER,
                "policy", policy.getId(), paymentReminderOf(policy));
        publishExpiryReminder(policy);
        return toVO(policyMapper.selectById(id));
    }

    @Override
    public void delete(Long memberId, Long id) {
        InsurancePolicy policy = requireOwnedPolicy(memberId, id);
        policyMapper.deleteById(id);
        eventPublisher.publish(Topics.TOPIC_REMINDER, EventTypes.REMINDER_CANCEL, "policy", id,
                cancelReminderOf(policy, "policy_payment"));
        eventPublisher.publish(Topics.TOPIC_REMINDER, EventTypes.REMINDER_CANCEL, "policy", id,
                cancelReminderOf(policy, "policy_expiry"));
        log.info("删除保单: id={}", id);
    }

    @Override
    public CoverageMapVO coverageMap(Long memberId) {
        List<FamilyMemberSnapshotDTO> members = ownershipGuard.listOwnedMembers(memberId);
        List<InsurancePolicy> activePolicies = policyMapper.selectList(
                new LambdaQueryWrapper<InsurancePolicy>()
                        .eq(InsurancePolicy::getOwnerMemberId, memberId)
                        .eq(InsurancePolicy::getStatus, 1));

        Map<Long, List<String>> covered = activePolicies.stream().collect(
                Collectors.groupingBy(InsurancePolicy::getInsuredMemberId,
                        Collectors.mapping(InsurancePolicy::getCategory,
                                Collectors.collectingAndThen(Collectors.toSet(), s -> CATEGORY_ORDER.stream()
                                        .filter(s::contains).toList()))));

        CoverageMapVO vo = new CoverageMapVO();
        List<CoverageMapVO.MemberRow> rows = new ArrayList<>();
        for (FamilyMemberSnapshotDTO m : members) {
            CoverageMapVO.MemberRow row = new CoverageMapVO.MemberRow();
            row.setMemberId(m.getId());
            row.setMemberName(m.getName());
            row.setRelation(m.getRelation());
            row.setCategories(covered.getOrDefault(m.getId(), List.of()));
            rows.add(row);
        }
        vo.setRows(rows);
        vo.setTotalAnnualPremium(activePolicies.stream()
                .filter(p -> "annual".equals(p.getPaymentFreq()) && p.getPremium() != null)
                .map(InsurancePolicy::getPremium)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        vo.setActivePolicyCount(activePolicies.size());
        return vo;
    }

    /** 按 id 取保单并校验归属：不存在报 400，他人保单 403 */
    private InsurancePolicy requireOwnedPolicy(Long memberId, Long id) {
        InsurancePolicy policy = policyMapper.selectById(id);
        if (policy == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "保单不存在");
        }
        if (!policy.getOwnerMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return policy;
    }

    /** 缴费提醒规则：下次缴费日前 7 天首次提醒，逐年重复至满期 */
    private com.xp.starsite.api.event.reminder.ReminderRegisterEvent paymentReminderOf(InsurancePolicy policy) {
        com.xp.starsite.api.event.reminder.ReminderRegisterEvent event =
                new com.xp.starsite.api.event.reminder.ReminderRegisterEvent();
        event.setSource("star-site-insurance");
        event.setBizType("policy_payment");
        event.setBizId(policy.getId());
        event.setFamilyId(policy.getFamilyId());
        event.setOwnerMemberId(policy.getOwnerMemberId());
        event.setTitle(policy.getProductName() + " 缴费");
        if (policy.getNextPaymentDate() != null) {
            event.setFirstRemindAt(policy.getNextPaymentDate().minusDays(7));
        }
        event.setRepeatType(com.xp.starsite.api.event.reminder.ReminderRepeatType.ANNUAL);
        event.setRepeatUntil(policy.getMaturityDate());
        event.setAdvanceDays(7);
        return event;
    }

    /** 到期提醒规则：满期日前 30 天单次提醒（终身险不发） */
    private void publishExpiryReminder(InsurancePolicy policy) {
        if (policy.getMaturityDate() == null) {
            return;
        }
        com.xp.starsite.api.event.reminder.ReminderRegisterEvent event =
                new com.xp.starsite.api.event.reminder.ReminderRegisterEvent();
        event.setSource("star-site-insurance");
        event.setBizType("policy_expiry");
        event.setBizId(policy.getId());
        event.setFamilyId(policy.getFamilyId());
        event.setOwnerMemberId(policy.getOwnerMemberId());
        event.setTitle(policy.getProductName() + " 满期");
        event.setFirstRemindAt(policy.getMaturityDate().minusDays(30));
        event.setRepeatType(com.xp.starsite.api.event.reminder.ReminderRepeatType.NONE);
        event.setAdvanceDays(30);
        eventPublisher.publish(Topics.TOPIC_REMINDER, EventTypes.REMINDER_REGISTER,
                "policy", policy.getId(), event);
    }

    private com.xp.starsite.api.event.reminder.ReminderCancelEvent cancelReminderOf(
            InsurancePolicy policy, String bizType) {
        com.xp.starsite.api.event.reminder.ReminderCancelEvent event =
                new com.xp.starsite.api.event.reminder.ReminderCancelEvent();
        event.setSource("star-site-insurance");
        event.setBizType(bizType);
        event.setBizId(policy.getId());
        return event;
    }

    private void applyDto(InsurancePolicy policy, PolicySaveDTO dto) {
        if (!CATEGORIES.contains(dto.getCategory())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "不支持的险种类型");
        }
        policy.setCompany(dto.getCompany());
        policy.setProductName(dto.getProductName());
        policy.setPolicyNo(dto.getPolicyNo());
        policy.setCategory(dto.getCategory());
        policy.setInsuredMemberId(dto.getInsuredMemberId());
        policy.setStartDate(dto.getStartDate());
        policy.setMaturityDate(dto.getMaturityDate());
        policy.setPremium(dto.getPremium());
        policy.setPaymentFreq(StringUtils.hasText(dto.getPaymentFreq()) ? dto.getPaymentFreq() : "annual");
        policy.setNextPaymentDate(dto.getNextPaymentDate());
        policy.setCoverageAmount(dto.getCoverageAmount());
        policy.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        policy.setFileId(dto.getFileId());
        policy.setRemark(dto.getRemark());
    }

    private PolicyVO toVO(InsurancePolicy policy) {
        PolicyVO vo = new PolicyVO();
        BeanUtils.copyProperties(policy, vo);
        return vo;
    }
}
