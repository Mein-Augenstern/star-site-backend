package com.xp.starsite.reminder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xp.starsite.api.event.reminder.ReminderCancelEvent;
import com.xp.starsite.api.event.reminder.ReminderRegisterEvent;
import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.api.dto.FamilyMemberSnapshotDTO;
import com.xp.starsite.reminder.dto.UserRuleCreateDTO;
import com.xp.starsite.api.dto.ReminderRuleDTO;
import com.xp.starsite.reminder.entity.Notification;
import com.xp.starsite.reminder.vo.UserRuleVO;
import com.xp.starsite.reminder.entity.ReminderOccurrence;
import com.xp.starsite.reminder.entity.ReminderRule;
import com.xp.starsite.reminder.mapper.NotificationMapper;
import com.xp.starsite.reminder.mapper.ReminderOccurrenceMapper;
import com.xp.starsite.reminder.mapper.ReminderRuleMapper;
import com.xp.starsite.reminder.service.ReminderService;
import com.xp.starsite.reminder.vo.NotificationVO;
import com.xp.starsite.reminder.vo.OccurrenceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 提醒中心实现。
 * <p>
 * 调度模型（取舍见架构文档 D3）：每日扫描补生成未来 N 天实例（(rule_id,due_date)
 * 唯一键幂等），当天到期的 PENDING 实例投站内通知——天粒度满足缴费/到期场景；
 * 延迟消息精确投递留作 V2 自定义"某日某时"提醒的练兵场景。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

    /** 状态：待提醒/已通知/已完成/已忽略 */
    private static final int ST_PENDING = 1;
    private static final int ST_NOTIFIED = 2;
    private static final int ST_DONE = 3;
    private static final int ST_DISMISSED = 4;

    private final ReminderRuleMapper ruleMapper;
    private final ReminderOccurrenceMapper occurrenceMapper;
    private final NotificationMapper notificationMapper;
    private final FamilyOwnershipGuard ownershipGuard;
    /** 邮件渠道可选：未配 spring.mail.host 时不装配（null = 仅站内信） */
    private final org.springframework.beans.factory.ObjectProvider<com.xp.starsite.reminder.notify.EmailChannel> emailChannel;

    @Override
    @Transactional
    public void upsertRule(ReminderRegisterEvent event) {
        ReminderRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<ReminderRule>()
                .eq(ReminderRule::getSource, event.getSource())
                .eq(ReminderRule::getBizType, event.getBizType())
                .eq(ReminderRule::getBizId, event.getBizId()));
        boolean isNew = rule == null;
        if (isNew) {
            rule = new ReminderRule();
            rule.setSource(event.getSource());
            rule.setBizType(event.getBizType());
            rule.setBizId(event.getBizId());
        }
        rule.setFamilyId(event.getFamilyId());
        rule.setOwnerMemberId(event.getOwnerMemberId());
        rule.setTitle(event.getTitle());
        rule.setFirstRemindAt(event.getFirstRemindAt());
        rule.setRepeatType(event.getRepeatType() == null ? "NONE" : event.getRepeatType().name());
        rule.setRepeatUntil(event.getRepeatUntil());
        rule.setAdvanceDays(event.getAdvanceDays() == null ? 0 : event.getAdvanceDays());
        rule.setStatus(1);
        if (isNew) {
            ruleMapper.insert(rule);
        } else {
            ruleMapper.updateById(rule);
        }
        // 立即补生成首期实例（扫描任务兜底，uk 防重）
        generateOccurrences(rule, LocalDate.now(), LocalDate.now().plusDays(30));
        log.info("提醒规则 upsert: rule={}, {}.{}.{}, first={}",
                rule.getId(), rule.getSource(), rule.getBizType(), rule.getBizId(), rule.getFirstRemindAt());
    }

    @Override
    @Transactional
    public void upsertRuleFromInternal(com.xp.starsite.api.dto.ReminderRuleDTO dto) {
        ReminderRegisterEvent event = new ReminderRegisterEvent();
        event.setSource(dto.getSource());
        event.setBizType(dto.getBizType());
        event.setBizId(dto.getBizId());
        event.setFamilyId(dto.getFamilyId());
        event.setOwnerMemberId(dto.getOwnerMemberId());
        event.setTitle(dto.getTitle());
        event.setFirstRemindAt(dto.getRemindDate());
        event.setRepeatType(dto.getRepeatType() == null
                ? com.xp.starsite.api.event.reminder.ReminderRepeatType.NONE
                : com.xp.starsite.api.event.reminder.ReminderRepeatType.valueOf(dto.getRepeatType()));
        event.setRepeatUntil(dto.getRepeatUntil());
        event.setAdvanceDays(dto.getAdvanceDays() == null ? 0 : dto.getAdvanceDays());
        upsertRule(event);
    }

    @Override
    @Transactional
    public void deleteRuleFromInternal(com.xp.starsite.api.dto.ReminderRuleDTO dto) {
        ReminderCancelEvent event = new ReminderCancelEvent();
        event.setSource(dto.getSource());
        event.setBizType(dto.getBizType());
        event.setBizId(dto.getBizId());
        cancelRule(event);
    }

    @Override
    @Transactional
    public void cancelRule(ReminderCancelEvent event) {
        ReminderRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<ReminderRule>()
                .eq(ReminderRule::getSource, event.getSource())
                .eq(ReminderRule::getBizType, event.getBizType())
                .eq(ReminderRule::getBizId, event.getBizId()));
        if (rule == null) {
            return;
        }
        // 未完成实例一并清掉；已投递的通知保留（历史）
        occurrenceMapper.delete(new LambdaQueryWrapper<ReminderOccurrence>()
                .eq(ReminderOccurrence::getRuleId, rule.getId())
                .in(ReminderOccurrence::getStatus, ST_PENDING, ST_NOTIFIED));
        ruleMapper.deleteById(rule.getId());
        log.info("提醒规则撤销: {}.{}.{}", event.getSource(), event.getBizType(), event.getBizId());
    }

    private static final String USER_SOURCE = "user";
    private static final String USER_BIZ_TYPE = "user_rule";

    @Override
    @Transactional
    public UserRuleVO createUserRule(Long memberId, UserRuleCreateDTO dto) {
        FamilyMemberSnapshotDTO snapshot =
                ownershipGuard.requireOwned(memberId, dto.getFamilyMemberId());
        // 幂等键 source+bizType+bizId 需唯一：先落行占 id，再回填 biz_id=规则自身 id
        ReminderRule rule = new ReminderRule();
        rule.setSource(USER_SOURCE);
        rule.setBizType(USER_BIZ_TYPE);
        rule.setBizId(0L);
        rule.setFamilyId(snapshot.getFamilyId());
        rule.setFamilyMemberId(dto.getFamilyMemberId());
        rule.setOwnerMemberId(memberId);
        rule.setTitle(dto.getTitle());
        rule.setFirstRemindAt(dto.getFirstRemindAt());
        rule.setRepeatType(StringUtils.hasText(dto.getRepeatType()) ? dto.getRepeatType() : "NONE");
        rule.setRepeatUntil(dto.getRepeatUntil());
        rule.setAdvanceDays(0);
        rule.setStatus(1);
        ruleMapper.insert(rule);
        rule.setBizId(rule.getId());
        ruleMapper.updateById(rule);
        generateOccurrences(rule, LocalDate.now(), LocalDate.now().plusDays(30));
        log.info("用户自建提醒: rule={}, member={}, for={}, title={}",
                rule.getId(), memberId, dto.getFamilyMemberId(), dto.getTitle());
        return toUserRuleVO(rule, 0L);
    }

    @Override
    public List<UserRuleVO> listUserRules(Long memberId) {
        Set<Long> owned = Set.copyOf(ownershipGuard.listOwnedMemberIds(memberId));
        if (owned.isEmpty()) {
            return List.of();
        }
        List<ReminderRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<ReminderRule>()
                .eq(ReminderRule::getSource, USER_SOURCE)
                .in(ReminderRule::getFamilyMemberId, owned)
                .eq(ReminderRule::getStatus, 1)
                .orderByDesc(ReminderRule::getId));
        return rules.stream().map(r -> toUserRuleVO(r, countPending(r.getId()))).toList();
    }

    @Override
    @Transactional
    public void deleteUserRule(Long memberId, Long ruleId) {
        ReminderRule rule = requireOwnedRule(memberId, ruleId);
        occurrenceMapper.delete(new LambdaQueryWrapper<ReminderOccurrence>()
                .eq(ReminderOccurrence::getRuleId, rule.getId())
                .in(ReminderOccurrence::getStatus, ST_PENDING, ST_NOTIFIED));
        ruleMapper.deleteById(rule.getId());
        log.info("用户自建提醒删除: rule={}", rule.getId());
    }

    /** 校验自建规则归属：规则必须属于我的家庭成员 */
    private ReminderRule requireOwnedRule(Long memberId, Long ruleId) {
        ReminderRule rule = ruleMapper.selectById(ruleId);
        if (rule == null || !USER_SOURCE.equals(rule.getSource())) {
            throw new com.xp.starsite.common.exception.BizException(404, "提醒规则不存在");
        }
        Set<Long> owned = Set.copyOf(ownershipGuard.listOwnedMemberIds(memberId));
        if (rule.getFamilyMemberId() == null || !owned.contains(rule.getFamilyMemberId())) {
            throw new com.xp.starsite.common.exception.BizException(403, "无权限操作该提醒规则");
        }
        return rule;
    }

    private Long countPending(Long ruleId) {
        return occurrenceMapper.selectCount(new LambdaQueryWrapper<ReminderOccurrence>()
                .eq(ReminderOccurrence::getRuleId, ruleId)
                .in(ReminderOccurrence::getStatus, ST_PENDING, ST_NOTIFIED));
    }

    private UserRuleVO toUserRuleVO(ReminderRule rule, Long pendingCount) {
        UserRuleVO vo = new UserRuleVO();
        BeanUtils.copyProperties(rule, vo);
        vo.setPendingCount(pendingCount);
        return vo;
    }

    @Override
    public int scanAndNotify() {
        LocalDate today = LocalDate.now();
        List<ReminderRule> rules = ruleMapper.selectList(
                new LambdaQueryWrapper<ReminderRule>().eq(ReminderRule::getStatus, 1));
        for (ReminderRule rule : rules) {
            generateOccurrences(rule, today, today.plusDays(30));
        }

        // 投递今天（含逾期未投）到期的实例
        List<ReminderOccurrence> due = occurrenceMapper.selectList(
                new LambdaQueryWrapper<ReminderOccurrence>()
                        .eq(ReminderOccurrence::getStatus, ST_PENDING)
                        .le(ReminderOccurrence::getDueDate, today));
        var channel = emailChannel.getIfAvailable();
        for (ReminderOccurrence occ : due) {
            Notification n = new Notification();
            n.setOccurrenceId(occ.getId());
            n.setOwnerMemberId(occ.getOwnerMemberId());
            n.setTitle(occ.getTitle());
            n.setReadFlag(0);
            notificationMapper.insert(n);
            occ.setStatus(ST_NOTIFIED);
            occ.setNotifiedAt(java.time.LocalDateTime.now());
            occurrenceMapper.updateById(occ);
            // 邮件触达（可选渠道）：失败不影响站内信
            if (channel != null && channel.applicable(occ.getOwnerMemberId())) {
                channel.send(occ.getOwnerMemberId(), occ.getTitle(), occ.getDueDate().toString());
            }
        }
        if (!due.isEmpty()) {
            log.info("提醒投递: {} 条", due.size());
        }
        return due.size();
    }

    @Override
    public List<OccurrenceVO> upcoming(Long memberId, int days) {
        LocalDate today = LocalDate.now();
        return occurrenceMapper.selectList(new LambdaQueryWrapper<ReminderOccurrence>()
                        .eq(ReminderOccurrence::getOwnerMemberId, memberId)
                        .le(ReminderOccurrence::getDueDate, today.plusDays(days))
                        .in(ReminderOccurrence::getStatus, ST_PENDING, ST_NOTIFIED)
                        .orderByAsc(ReminderOccurrence::getDueDate))
                .stream().map(this::toOccurrenceVO).toList();
    }

    @Override
    public long unreadCount(Long memberId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getOwnerMemberId, memberId)
                .eq(Notification::getReadFlag, 0));
    }

    @Override
    public List<NotificationVO> notifications(Long memberId, long page, long size) {
        return notificationMapper.selectPage(
                        Page.of(page, size), new LambdaQueryWrapper<Notification>()
                                .eq(Notification::getOwnerMemberId, memberId)
                                .orderByDesc(Notification::getId))
                .getRecords().stream().map(this::toNotificationVO).toList();
    }

    @Override
    public void markRead(Long memberId, Long notificationId) {
        Notification n = notificationMapper.selectById(notificationId);
        if (n == null || !n.getOwnerMemberId().equals(memberId)) {
            return;
        }
        n.setReadFlag(1);
        notificationMapper.updateById(n);
    }

    @Override
    public void completeOccurrence(Long memberId, Long occurrenceId) {
        updateStatus(memberId, occurrenceId, ST_DONE);
    }

    @Override
    public void dismissOccurrence(Long memberId, Long occurrenceId) {
        updateStatus(memberId, occurrenceId, ST_DISMISSED);
    }

    private void updateStatus(Long memberId, Long occurrenceId, int status) {
        ReminderOccurrence occ = occurrenceMapper.selectById(occurrenceId);
        if (occ == null || !occ.getOwnerMemberId().equals(memberId)) {
            return;
        }
        occ.setStatus(status);
        occurrenceMapper.updateById(occ);
    }

    /** 按规则的重复语义，生成 [from, to] 窗口内的实例（uk 幂等，已存在则跳过） */
    private void generateOccurrences(ReminderRule rule, LocalDate from, LocalDate to) {
        if (rule.getFirstRemindAt() == null) {
            return;
        }
        for (LocalDate due : dueDatesOf(rule, from, to)) {
            Long exists = occurrenceMapper.selectCount(new LambdaQueryWrapper<ReminderOccurrence>()
                    .eq(ReminderOccurrence::getRuleId, rule.getId())
                    .eq(ReminderOccurrence::getDueDate, due));
            if (exists > 0) {
                continue;
            }
            ReminderOccurrence occ = new ReminderOccurrence();
            occ.setRuleId(rule.getId());
            occ.setFamilyId(rule.getFamilyId());
            occ.setOwnerMemberId(rule.getOwnerMemberId());
            occ.setTitle(rule.getTitle());
            occ.setSource(rule.getSource());
            occ.setBizType(rule.getBizType());
            occ.setBizId(rule.getBizId());
            occ.setDueDate(due);
            occ.setStatus(ST_PENDING);
            try {
                occurrenceMapper.insert(occ);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 并发/重放下的幂等兜底
                log.debug("实例已存在，跳过: rule={}, due={}", rule.getId(), due);
            }
        }
    }

    /** 计算规则在窗口内的应提醒日期集合（first_remind_at 本身 + 重复扩展点） */
    private List<LocalDate> dueDatesOf(ReminderRule rule, LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate first = rule.getFirstRemindAt();
        LocalDate until = rule.getRepeatUntil();
        switch (StringUtils.hasText(rule.getRepeatType()) ? rule.getRepeatType() : "NONE") {
            case "ANNUAL" -> {
                for (LocalDate d = first; !d.isAfter(to); d = d.plusYears(1)) {
                    if (!d.isBefore(from) && (until == null || !d.isAfter(until))) {
                        dates.add(d);
                    }
                }
            }
            case "MONTHLY" -> {
                for (LocalDate d = first; !d.isAfter(to); d = d.plusMonths(1)) {
                    if (!d.isBefore(from) && (until == null || !d.isAfter(until))) {
                        dates.add(d);
                    }
                }
            }
            case "WEEKLY" -> {
                for (LocalDate d = first; !d.isAfter(to); d = d.plusWeeks(1)) {
                    if (!d.isBefore(from) && (until == null || !d.isAfter(until))) {
                        dates.add(d);
                    }
                }
            }
            default -> {
                // NONE：首期落在窗口内才提醒
                if (!first.isBefore(from) && !first.isAfter(to) && (until == null || !first.isAfter(until))) {
                    dates.add(first);
                }
            }
        }
        // 逾期补投：窗口内无应提醒日、但首期已过且规则未到期 → 今天补一期
        // （如缴费日前 7 天提醒、缴费日在 5 天后，则首期在今天之前，用户应立即收到提醒）
        if (dates.isEmpty() && first.isBefore(from) && (until == null || !until.isBefore(from))) {
            dates.add(from);
        }
        return dates;
    }

    private OccurrenceVO toOccurrenceVO(ReminderOccurrence occ) {
        OccurrenceVO vo = new OccurrenceVO();
        BeanUtils.copyProperties(occ, vo);
        return vo;
    }

    private NotificationVO toNotificationVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(n, vo);
        return vo;
    }
}
