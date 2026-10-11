package com.xp.starsite.finance.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.finance.dto.GoalSaveDTO;
import com.xp.starsite.finance.service.GoalService;
import com.xp.starsite.finance.vo.GoalVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 家庭目标接口（全部需 JWT）
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/finance/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public R<GoalVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                            @Valid @RequestBody GoalSaveDTO dto) {
        return R.ok(goalService.create(memberId, dto));
    }

    @GetMapping
    public R<List<GoalVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(goalService.list(memberId));
    }

    @PutMapping("/{id}")
    public R<GoalVO> update(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                            @PathVariable Long id,
                            @Valid @RequestBody GoalSaveDTO dto) {
        return R.ok(goalService.update(memberId, id, dto));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        goalService.delete(memberId, id);
        return R.ok();
    }
}
