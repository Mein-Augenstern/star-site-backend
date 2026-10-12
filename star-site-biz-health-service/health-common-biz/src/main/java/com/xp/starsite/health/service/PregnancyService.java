package com.xp.starsite.health.service;

import com.xp.starsite.health.dto.PregnancyUpsertDTO;
import com.xp.starsite.health.vo.PregnancyVO;

/**
 * 孕期助手服务：孕期档案（末次月经 LMP 锚点 + 孕前身高体重）的维护与计算视图。
 * <p>
 * 计算规则（以 LMP 为锚点）：孕周 = LMP 距今天数整除 7；预产期 = LMP + 280 天；
 * 孕早期 ≤12 周 / 孕中期 13-27 周 / 孕晚期 ≥28 周；增重建议按 IOM 单胎标准分档（BMI）。
 * 产检里程碑（NT/唐筛/大排畸/糖耐等 8 个窗口）随计算视图返回并标注 pending/current/done。
 * 归属防线约定同 {@link ReportService}（首参 memberId + family 归属校验，fail-closed）。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface PregnancyService {

    /**
     * 设置/更新孕期档案（一人一份，重复提交覆盖）
     *
     * @return 含孕周/预产期/阶段/产检计划/增重建议的计算视图
     * @throws com.xp.starsite.common.exception.BizException 403 家庭成员不归属当前账号
     */
    PregnancyVO upsertPregnancy(Long memberId, PregnancyUpsertDTO dto);

    /**
     * 孕期档案查询
     *
     * @return 计算视图；无档案返回 null（data 为空，code=0）
     * @throws com.xp.starsite.common.exception.BizException 403 家庭成员不归属当前账号
     */
    PregnancyVO getPregnancy(Long memberId, Long familyMemberId);
}
