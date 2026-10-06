package com.xp.starsite.family.service;

import com.xp.starsite.family.dto.FamilyCreateDTO;
import com.xp.starsite.family.dto.FamilyMemberDTO;
import com.xp.starsite.family.vo.FamilyMemberVO;
import com.xp.starsite.family.vo.FamilyVO;

import java.util.List;

/**
 * 家庭档案服务
 */
public interface FamilyService {

    /** 创建家庭（一个账号只能有一个家庭） */
    FamilyVO create(Long ownerMemberId, FamilyCreateDTO dto);

    /** 我的家庭，没有则为 null */
    FamilyVO mine(Long ownerMemberId);

    /** 添加成员档案 */
    FamilyMemberVO addMember(Long ownerMemberId, FamilyMemberDTO dto);

    /** 成员列表 */
    List<FamilyMemberVO> listMembers(Long ownerMemberId);

    /** 编辑成员档案 */
    FamilyMemberVO updateMember(Long ownerMemberId, Long memberId, FamilyMemberDTO dto);

    /** 删除成员档案（逻辑删） */
    void deleteMember(Long ownerMemberId, Long memberId);
}
