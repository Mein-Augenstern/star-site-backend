package com.xp.starsite.finance.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.finance.controller.actions.FinanceActions;
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
 * <p>
 * 业务逻辑统一经模板包裹：模板包掉成功与失败的全部响应处理（成功 R.ok / 失败 R.fail），
 * Controller 一行返回；动作命名规范 finance.goal.*。
 *
 * @author muyi
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/finance/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 新建储蓄目标
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param dto      目标内容（名称/目标金额/期限等）
     * @return 新建目标视图
     */
    @PostMapping
    public R<GoalVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                            @Valid @RequestBody GoalSaveDTO dto) {
        return writeTemplate.write(FinanceActions.GOAL_CREATE,
                () -> goalService.create(memberId, dto));
    }

    /**
     * 我的目标列表（含进度）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @return 目标视图列表
     */
    @GetMapping
    public R<List<GoalVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query(FinanceActions.GOAL_LIST, () -> goalService.list(memberId));
    }

    /**
     * 修改目标（含存入/进度更新）
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param id       目标 id
     * @param dto      目标更新内容
     * @return 更新后的目标视图
     */
    @PutMapping("/{id}")
    public R<GoalVO> update(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                            @PathVariable Long id,
                            @Valid @RequestBody GoalSaveDTO dto) {
        return writeTemplate.write(FinanceActions.GOAL_UPDATE,
                () -> goalService.update(memberId, id, dto));
    }

    /**
     * 删除目标
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param id       目标 id
     * @return 删除成功（无业务数据）
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        return writeTemplate.write(FinanceActions.GOAL_DELETE,
                () -> goalService.delete(memberId, id));
    }
}
