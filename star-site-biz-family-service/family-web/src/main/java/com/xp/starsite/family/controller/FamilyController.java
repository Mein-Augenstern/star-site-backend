package com.xp.starsite.family.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.family.controller.actions.FamilyActions;
import com.xp.starsite.family.dto.FamilyCreateDTO;
import com.xp.starsite.family.dto.FamilyMemberDTO;
import com.xp.starsite.family.service.FamilyService;
import com.xp.starsite.family.vo.FamilyMemberVO;
import com.xp.starsite.family.vo.FamilyVO;
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
 * 家庭档案接口：家庭建档、成员档案（自然人）增删改查。
 * 全部端点需 JWT；业务逻辑统一经模板包裹（动作名 family.*），
 * 归属防线由 Service 层 FamilyOwnershipGuard 承担（fail-closed）。
 *
 * @author muyi
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/family")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 创建家庭（一个顶梁柱一个家）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param dto      家庭信息（名称/备注）
     * @return 新建家庭视图
     */
    @PostMapping
    public R<FamilyVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                              @Valid @RequestBody FamilyCreateDTO dto) {
        return writeTemplate.write(FamilyActions.CREATE,
                () -> familyService.create(memberId, dto));
    }

    /**
     * 我的家庭（未建返回 data=null）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @return 家庭视图，未建为 null
     */
    @GetMapping("/mine")
    public R<FamilyVO> mine(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query(FamilyActions.MINE,
                () -> familyService.mine(memberId));
    }

    /**
     * 新增家庭成员档案
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param dto      成员档案内容
     * @return 新建成员视图
     */
    @PostMapping("/members")
    public R<FamilyMemberVO> addMember(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                       @Valid @RequestBody FamilyMemberDTO dto) {
        return writeTemplate.write(FamilyActions.MEMBER_ADD,
                () -> familyService.addMember(memberId, dto));
    }

    /**
     * 家庭成员档案列表
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @return 全部成员视图
     */
    @GetMapping("/members")
    public R<List<FamilyMemberVO>> listMembers(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query(FamilyActions.MEMBER_LIST,
                () -> familyService.listMembers(memberId));
    }

    /**
     * 更新家庭成员档案
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param id       成员档案 id
     * @param dto      成员档案内容
     * @return 更新后的成员视图
     */
    @PutMapping("/members/{id}")
    public R<FamilyMemberVO> updateMember(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                          @PathVariable Long id,
                                          @Valid @RequestBody FamilyMemberDTO dto) {
        return writeTemplate.write(FamilyActions.MEMBER_UPDATE,
                () -> familyService.updateMember(memberId, id, dto));
    }

    /**
     * 删除家庭成员档案（逻辑删）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param id       成员档案 id
     * @return 空
     */
    @DeleteMapping("/members/{id}")
    public R<Void> deleteMember(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                @PathVariable Long id) {
        return writeTemplate.write(FamilyActions.MEMBER_DELETE,
                () -> familyService.deleteMember(memberId, id));
    }
}
