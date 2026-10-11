package com.xp.starsite.insurance.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
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
 * 保单接口（全部需 JWT；memberId 用于归属校验，防横向越权）
 */
@RestController
@RequestMapping("/api/insurance")
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceService insuranceService;

    @PostMapping("/policies")
    public R<PolicyVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                              @Valid @RequestBody PolicySaveDTO dto) {
        return R.ok(insuranceService.create(memberId, dto));
    }

    @GetMapping("/policies")
    public R<PageVO<PolicyVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                    @RequestParam(required = false) Long insuredMemberId,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "10") long size) {
        return R.ok(insuranceService.list(memberId, insuredMemberId, category, status, page, size));
    }

    @GetMapping("/policies/{id}")
    public R<PolicyVO> detail(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                              @PathVariable Long id) {
        return R.ok(insuranceService.getDetail(memberId, id));
    }

    @PutMapping("/policies/{id}")
    public R<PolicyVO> update(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                              @PathVariable Long id,
                              @Valid @RequestBody PolicySaveDTO dto) {
        return R.ok(insuranceService.update(memberId, id, dto));
    }

    @DeleteMapping("/policies/{id}")
    public R<Void> delete(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        insuranceService.delete(memberId, id);
        return R.ok();
    }

    @GetMapping("/coverage-map")
    public R<CoverageMapVO> coverageMap(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(insuranceService.coverageMap(memberId));
    }
}
