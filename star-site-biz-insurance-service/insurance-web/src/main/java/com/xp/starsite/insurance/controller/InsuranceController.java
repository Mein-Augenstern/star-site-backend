package com.xp.starsite.insurance.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.insurance.controller.actions.InsuranceActions;
import com.xp.starsite.insurance.dto.PolicySaveDTO;
import com.xp.starsite.insurance.service.InsuranceService;
import com.xp.starsite.insurance.vo.CoverageMapVO;
import com.xp.starsite.insurance.vo.PageVO;
import com.xp.starsite.insurance.vo.PolicyVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 保单接口：保单 CRUD、保障地图。
 * 全部端点需 JWT；memberId 用于归属校验防横向越权；
 * 业务逻辑统一经模板包裹（动作名 insurance.policy.* / insurance.map）。
 *
 * @author muyi
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/insurance")
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceService insuranceService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 录入保单（同时注册缴费提醒规则）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param dto      保单内容
     * @return 新建保单视图
     */
    @PostMapping("/policies")
    public R<PolicyVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                              @Valid @RequestBody PolicySaveDTO dto) {
        return writeTemplate.write(InsuranceActions.POLICY_CREATE,
                () -> insuranceService.create(memberId, dto));
    }

    /**
     * 保单分页（可按被保人/险种/状态过滤）
     *
     * @param memberId        当前登录账号（JWT 注入）
     * @param insuredMemberId 可选，按被保人过滤
     * @param category        可选，按险种过滤
     * @param status          可选，按状态过滤
     * @param page            页码，从 1 起
     * @param size            每页条数
     * @return 保单分页
     */
    @GetMapping("/policies")
    public R<PageVO<PolicyVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                    @RequestParam(required = false) Long insuredMemberId,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "10") long size) {
        return queryTemplate.query(InsuranceActions.POLICY_PAGE,
                () -> insuranceService.list(memberId, insuredMemberId, category, status, page, size));
    }

    /**
     * 保单详情
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param id       保单 id
     * @return 保单视图
     */
    @GetMapping("/policies/{id}")
    public R<PolicyVO> detail(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                              @PathVariable Long id) {
        return queryTemplate.query(InsuranceActions.POLICY_DETAIL,
                () -> insuranceService.getDetail(memberId, id));
    }

    /**
     * 更新保单
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param id       保单 id
     * @param dto      保单内容
     * @return 更新后的保单视图
     */
    @PutMapping("/policies/{id}")
    public R<PolicyVO> update(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                              @PathVariable Long id,
                              @Valid @RequestBody PolicySaveDTO dto) {
        return writeTemplate.write(InsuranceActions.POLICY_UPDATE,
                () -> insuranceService.update(memberId, id, dto));
    }

    /**
     * 删除保单（逻辑删，同步撤销提醒规则）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param id       保单 id
     * @return 空
     */
    @DeleteMapping("/policies/{id}")
    public R<Void> delete(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        return writeTemplate.write(InsuranceActions.POLICY_DELETE,
                () -> insuranceService.delete(memberId, id));
    }

    /**
     * 保障地图（按家庭成员聚合的保障概览）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @return 保障地图视图
     */
    @GetMapping("/coverage-map")
    public R<CoverageMapVO> coverageMap(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query(InsuranceActions.COVERAGE_MAP,
                () -> insuranceService.coverageMap(memberId));
    }
}
