package com.xp.starsite.health.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.health.controller.actions.HealthActions;
import com.xp.starsite.health.dto.PregnancyUpsertDTO;
import com.xp.starsite.health.service.PregnancyService;
import com.xp.starsite.health.vo.PregnancyVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 孕期助手接口：孕期档案维护与计算视图（孕周/预产期/产检里程碑/增重建议）。
 * 计算规则见 {@link com.xp.starsite.health.service.PregnancyService} 类注释；
 * 拆分约定与鉴权说明见 {@link ReportController} 类注释；
 * 业务逻辑统一经模板包裹（动作名 health.pregnancy.*）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class PregnancyController {

    private final PregnancyService pregnancyService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 设置/更新孕期档案（一人一份，重复提交覆盖；
     * 保存后自动按里程碑窗口注册产检提醒，提醒中心不可用不阻断保存）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param dto      孕期档案内容
     * @return 保存后的孕期计算视图（孕周/预产期/里程碑/增重建议）
     */
    @PostMapping("/pregnancy")
    public R<PregnancyVO> upsertPregnancy(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                          @Valid @RequestBody PregnancyUpsertDTO dto) {
        return writeTemplate.write(HealthActions.PREGNANCY_UPSERT,
                () -> pregnancyService.upsertPregnancy(memberId, dto));
    }

    /**
     * 孕期档案查询（无档案返回 data=null）
     *
     * @param memberId       当前登录账号（JWT 注入）
     * @param familyMemberId 指定成员
     * @return 孕期计算视图，无档案时为 null
     */
    @GetMapping("/pregnancy")
    public R<PregnancyVO> getPregnancy(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                       @RequestParam Long familyMemberId) {
        return queryTemplate.query(HealthActions.PREGNANCY_GET,
                () -> pregnancyService.getPregnancy(memberId, familyMemberId));
    }
}
