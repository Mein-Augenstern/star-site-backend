package com.xp.starsite.family.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
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
 * 家庭档案接口（全部需 JWT）
 */
@RestController
@RequestMapping("/api/family")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    @PostMapping
    public R<FamilyVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                              @Valid @RequestBody FamilyCreateDTO dto) {
        return R.ok(familyService.create(memberId, dto));
    }

    @GetMapping("/mine")
    public R<FamilyVO> mine(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(familyService.mine(memberId));
    }

    @PostMapping("/members")
    public R<FamilyMemberVO> addMember(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                       @Valid @RequestBody FamilyMemberDTO dto) {
        return R.ok(familyService.addMember(memberId, dto));
    }

    @GetMapping("/members")
    public R<List<FamilyMemberVO>> listMembers(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(familyService.listMembers(memberId));
    }

    @PutMapping("/members/{id}")
    public R<FamilyMemberVO> updateMember(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                          @PathVariable Long id,
                                          @Valid @RequestBody FamilyMemberDTO dto) {
        return R.ok(familyService.updateMember(memberId, id, dto));
    }

    @DeleteMapping("/members/{id}")
    public R<Void> deleteMember(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                @PathVariable Long id) {
        familyService.deleteMember(memberId, id);
        return R.ok();
    }
}
