package com.xp.starsite.finance.service.impl;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.common.util.IdGenerator;
import com.xp.starsite.finance.dto.GoalSaveDTO;
import com.xp.starsite.finance.entity.FinanceGoal;
import com.xp.starsite.finance.repository.FinanceGoalRepository;
import com.xp.starsite.finance.service.GoalService;
import com.xp.starsite.finance.vo.GoalVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * 家庭目标实现（V1 进度手动更新；F5 可联动账户/流水自动攒）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private final FinanceGoalRepository goalRepository;
    private final IdGenerator idGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GoalVO create(Long memberId, GoalSaveDTO dto) {
        FinanceGoal goal = new FinanceGoal();
        goal.setId(idGenerator.nextId());
        goal.setOwnerMemberId(memberId);
        goal.setFamilyId(0L);
        applyDto(goal, dto);
        goalRepository.save(goal);
        log.info("创建目标: id={}, name={}", goal.getId(), goal.getName());
        return toVO(goal);
    }

    @Override
    public List<GoalVO> list(Long memberId) {
        return goalRepository.listByOwner(memberId).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GoalVO update(Long memberId, Long id, GoalSaveDTO dto) {
        FinanceGoal goal = requireOwnedGoal(memberId, id);
        applyDto(goal, dto);
        goalRepository.update(goal);
        return toVO(goal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long memberId, Long id) {
        requireOwnedGoal(memberId, id);
        goalRepository.markDeleted(id);
    }

    private FinanceGoal requireOwnedGoal(Long memberId, Long id) {
        FinanceGoal goal = goalRepository.findById(id).orElse(null);
        if (goal == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "目标不存在");
        }
        if (!goal.getOwnerMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return goal;
    }

    private void applyDto(FinanceGoal goal, GoalSaveDTO dto) {
        goal.setName(dto.getName());
        goal.setTargetAmount(dto.getTargetAmount());
        goal.setCurrentAmount(dto.getCurrentAmount() == null ? BigDecimal.ZERO : dto.getCurrentAmount());
        goal.setDeadline(dto.getDeadline());
        goal.setRemark(dto.getRemark());
    }

    private GoalVO toVO(FinanceGoal goal) {
        GoalVO vo = new GoalVO();
        vo.setId(goal.getId());
        vo.setName(goal.getName());
        vo.setTargetAmount(goal.getTargetAmount());
        vo.setCurrentAmount(goal.getCurrentAmount());
        vo.setDeadline(goal.getDeadline());
        vo.setProgress(goal.getTargetAmount().signum() == 0 ? null
                : goal.getCurrentAmount().divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP));
        vo.setRemark(goal.getRemark());
        return vo;
    }
}
