package com.xp.starsite.health.service.impl;

import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.health.dto.PregnancyUpsertDTO;
import com.xp.starsite.health.entity.PregnancyProfile;
import com.xp.starsite.health.integration.PregnancyReminderBridge;
import com.xp.starsite.health.mapper.PregnancyProfileMapper;
import com.xp.starsite.health.service.PregnancyService;
import com.xp.starsite.health.vo.PregnancyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 孕期助手服务实现。计算规则见 {@link com.xp.starsite.health.service.PregnancyService} 类注释。
 * 档案落库后经 {@link PregnancyReminderBridge} 将产检里程碑注册为提醒中心规则（幂等 upsert）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PregnancyServiceImpl implements PregnancyService {

    /** 标准产检/关键节点（孕周窗口）：NT、唐筛、大排畸、糖耐是四大关键筛查 */
    private static final Object[][] MILESTONES = {
            {6, 8, "首次检查·建档B超", "确认宫内孕、胎心"},
            {11, 13, "NT 检查（早期唐筛）", "孕 11-13+6 窗口，勿错过"},
            {15, 20, "唐筛 / 无创 DNA", "中期血清学筛查"},
            {20, 24, "大排畸（系统超声）", "胎儿结构筛查"},
            {24, 28, "糖耐量试验 OGTT", "妊娠糖尿病筛查，需空腹"},
            {28, 32, "小排畸·胎心", "复查生长发育"},
            {32, 36, "胎心监护起步", "每 2 周产检"},
            {37, 40, "足月·每周产检", "随时准备待产"},
    };

    /** 预产期天数（孕 40 周） */
    private static final int DUE_DATE_DAYS = 280;

    /** 孕早期/中期/晚期的周数分界 */
    private static final int SECOND_TRIMESTER_FROM = 13;
    private static final int THIRD_TRIMESTER_FROM = 28;

    private final PregnancyProfileMapper pregnancyMapper;
    private final FamilyOwnershipGuard ownershipGuard;
    private final PregnancyReminderBridge reminderBridge;

    @Override
    public PregnancyVO upsertPregnancy(Long memberId, PregnancyUpsertDTO dto) {
        ownershipGuard.requireOwned(memberId, dto.getFamilyMemberId());
        PregnancyProfile profile = pregnancyMapper.selectByFamilyMemberId(dto.getFamilyMemberId());
        if (profile == null) {
            profile = new PregnancyProfile();
            profile.setFamilyMemberId(dto.getFamilyMemberId());
            profile.setEnded(0);
            profile.setLmpDate(dto.getLmpDate());
            profile.setHeightCm(dto.getHeightCm());
            profile.setPrePregnancyWeight(dto.getPrePregnancyWeight());
            pregnancyMapper.insert(profile);
        } else {
            profile.setLmpDate(dto.getLmpDate());
            profile.setHeightCm(dto.getHeightCm());
            profile.setPrePregnancyWeight(dto.getPrePregnancyWeight());
            pregnancyMapper.update(profile);
        }
        // 里程碑注册提醒规则（幂等，已过窗口自动跳过）；
        // 提醒中心不可用只告警不阻断——孕周保存是主链路，提醒注册是增强链路
        try {
            int registered = reminderBridge.registerPrenatalCheckups(
                    profile.getFamilyMemberId(), profile.getLmpDate(), null, memberId);
            log.info("孕期档案保存: member={}, 孕周锚点={}, 里程碑注册提醒 {} 条",
                    dto.getFamilyMemberId(), profile.getLmpDate(), registered);
        } catch (Exception e) {
            log.warn("产检里程碑提醒注册失败（不阻断档案保存）: member={}, err={}",
                    dto.getFamilyMemberId(), e.getMessage(), e);
        }
        return toVO(profile);
    }

    @Override
    public PregnancyVO getPregnancy(Long memberId, Long familyMemberId) {
        ownershipGuard.requireOwned(memberId, familyMemberId);
        PregnancyProfile profile = pregnancyMapper.selectByFamilyMemberId(familyMemberId);
        return profile == null ? null : toVO(profile);
    }

    /** 孕周中枢：以 LMP 为锚点计算孕周/预产期/阶段/产检计划/增重建议 */
    private PregnancyVO toVO(PregnancyProfile p) {
        PregnancyVO vo = new PregnancyVO();
        vo.setFamilyMemberId(p.getFamilyMemberId());
        vo.setLmpDate(p.getLmpDate());
        LocalDate today = LocalDate.now();
        long days = Math.max(0, ChronoUnit.DAYS.between(p.getLmpDate(), today));
        int week = (int) (days / 7);
        vo.setGestationalWeek(week);
        vo.setGestationalDay((int) (days % 7));
        vo.setDueDate(p.getLmpDate().plusDays(DUE_DATE_DAYS));
        vo.setDaysToDue(ChronoUnit.DAYS.between(today, vo.getDueDate()));
        vo.setStage(week <= SECOND_TRIMESTER_FROM - 1 ? "孕早期"
                : week <= THIRD_TRIMESTER_FROM - 1 ? "孕中期" : "孕晚期");

        applyWeightAdvice(vo, p);

        List<PregnancyVO.Milestone> ms = new ArrayList<>();
        for (Object[] m : MILESTONES) {
            PregnancyVO.Milestone item = new PregnancyVO.Milestone();
            int from = (int) m[0];
            int to = (int) m[1];
            item.setFromWeek(from);
            item.setToWeek(to);
            item.setName((String) m[2]);
            item.setTip((String) m[3]);
            item.setStatus(week < from ? "pending" : week <= to ? "current" : "done");
            ms.add(item);
        }
        vo.setMilestones(ms);
        return vo;
    }

    /** 孕前 BMI 与全孕期增重建议（IOM 单胎标准分档） */
    private void applyWeightAdvice(PregnancyVO vo, PregnancyProfile p) {
        if (p.getHeightCm() == null || p.getPrePregnancyWeight() == null
                || p.getHeightCm().signum() <= 0) {
            return;
        }
        double h = p.getHeightCm().doubleValue() / 100;
        double bmi = p.getPrePregnancyWeight().doubleValue() / (h * h);
        vo.setPreBmi(BigDecimal.valueOf(Math.round(bmi * 10) / 10.0));
        String advice = bmi < 18.5 ? "12.5~18.0"
                : bmi < 25 ? "11.5~16.0"
                : bmi < 30 ? "7.0~11.5"
                : "5.0~9.0";
        vo.setWeightGainAdvice(advice);
    }
}
