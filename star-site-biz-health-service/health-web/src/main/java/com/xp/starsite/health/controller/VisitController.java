package com.xp.starsite.health.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.health.dto.VisitCreateDTO;
import com.xp.starsite.health.service.VisitService;
import com.xp.starsite.health.vo.PageVO;
import com.xp.starsite.health.vo.VisitVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 就医记录接口：门诊/住院/产检就诊事件的登记、分页与删除。
 * 拆分约定与鉴权说明见 {@link ReportController} 类注释；
 * 业务逻辑统一经模板包裹（动作名 health.visit.*）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 新建就医记录
     */
    @PostMapping("/visits")
    public R<VisitVO> createVisit(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                  @Valid @RequestBody VisitCreateDTO dto) {
        return R.ok(writeTemplate.execute("health.visit.create",
                () -> visitService.createVisit(memberId, dto)));
    }

    /**
     * 就医记录分页列表（按就诊日期倒序；不传 familyMemberId 时限定全家成员范围）
     */
    @GetMapping("/visits")
    public R<PageVO<VisitVO>> listVisits(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                         @RequestParam(required = false) Long familyMemberId,
                                         @RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size) {
        return R.ok(queryTemplate.execute("health.visit.page",
                () -> visitService.listVisits(memberId, familyMemberId, page, size)));
    }

    /**
     * 删除就医记录（逻辑删）
     */
    @DeleteMapping("/visits/{id}")
    public R<Void> deleteVisit(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                               @PathVariable Long id) {
        writeTemplate.execute("health.visit.delete",
                () -> visitService.deleteVisit(memberId, id));
        return R.ok();
    }
}
