package com.xp.starsite.family.service.impl;

import com.xp.starsite.api.dto.FamilyMemberSnapshotDTO;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.family.dto.FamilyCreateDTO;
import com.xp.starsite.family.dto.FamilyMemberDTO;
import com.xp.starsite.family.entity.Family;
import com.xp.starsite.family.entity.FamilyMember;
import com.xp.starsite.family.mapper.FamilyMapper;
import com.xp.starsite.family.mapper.FamilyMemberMapper;
import com.xp.starsite.family.service.FamilyService;
import com.xp.starsite.family.vo.FamilyMemberVO;
import com.xp.starsite.family.vo.FamilyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 家庭档案服务实现。数据访问全部经 DAL（{@link FamilyMapper}/{@link FamilyMemberMapper}），
 * 条件与 SQL 下沉到 mapper XML，业务层零拼装。
 * 越权防线：所有成员读写先经 requireFamily/requireMember 收口在「本人家庭」内
 * （/internal/family 的归属校验是全平台越权防线，语义保持逐行等价）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyServiceImpl implements FamilyService {

    private final FamilyMapper familyMapper;
    private final FamilyMemberMapper familyMemberMapper;

    @Override
    public FamilyVO create(Long ownerMemberId, FamilyCreateDTO dto) {
        if (familyMapper.countByOwner(ownerMemberId) > 0) {
            throw new BizException(4005, "已有家庭，无需重复创建");
        }
        Family family = new Family();
        family.setName(dto.getName());
        family.setOwnerMemberId(ownerMemberId);
        family.setRemark(dto.getRemark());
        familyMapper.insert(family);
        log.info("创建家庭: id={}, owner={}", family.getId(), ownerMemberId);
        return toFamilyVO(familyMapper.selectById(family.getId()));
    }

    @Override
    public FamilyVO mine(Long ownerMemberId) {
        Family family = familyMapper.selectByOwner(ownerMemberId);
        return family == null ? null : toFamilyVO(family);
    }

    @Override
    public FamilyMemberVO addMember(Long ownerMemberId, FamilyMemberDTO dto) {
        Family family = requireFamily(ownerMemberId);
        FamilyMember member = new FamilyMember();
        member.setFamilyId(family.getId());
        applyDto(member, dto);
        familyMemberMapper.insert(member);
        return toVO(member);
    }

    @Override
    public List<FamilyMemberVO> listMembers(Long ownerMemberId) {
        Family family = requireFamily(ownerMemberId);
        return familyMemberMapper.selectByFamilyId(family.getId())
                .stream().map(this::toVO).toList();
    }

    @Override
    public FamilyMemberVO updateMember(Long ownerMemberId, Long memberId, FamilyMemberDTO dto) {
        Family family = requireFamily(ownerMemberId);
        FamilyMember member = requireMember(family.getId(), memberId);
        applyDto(member, dto);
        // 非空字段才更新（动态 SQL 语义同原 MyBatis-Plus updateById）
        familyMemberMapper.update(member);
        return toVO(familyMemberMapper.selectById(memberId));
    }

    @Override
    public void deleteMember(Long ownerMemberId, Long memberId) {
        Family family = requireFamily(ownerMemberId);
        FamilyMember member = requireMember(family.getId(), memberId);
        familyMemberMapper.markDeleted(member.getId());
    }

    @Override
    public FamilyMemberSnapshotDTO checkOwnership(Long ownerMemberId, Long familyMemberId) {
        Family family = familyMapper.selectByOwner(ownerMemberId);
        if (family == null) {
            return null;
        }
        FamilyMember member = familyMemberMapper.selectById(familyMemberId);
        return member == null || !member.getFamilyId().equals(family.getId())
                ? null : toSnapshot(member);
    }

    @Override
    public List<FamilyMemberSnapshotDTO> listMemberSnapshots(Long ownerMemberId) {
        Family family = familyMapper.selectByOwner(ownerMemberId);
        if (family == null) {
            return List.of();
        }
        return familyMemberMapper.selectByFamilyId(family.getId())
                .stream().map(this::toSnapshot).toList();
    }

    /** 前置：取当前账号的家庭，不存在报错提示先创建 */
    private Family requireFamily(Long ownerMemberId) {
        Family family = familyMapper.selectByOwner(ownerMemberId);
        if (family == null) {
            throw new BizException(4006, "请先创建家庭");
        }
        return family;
    }

    private FamilyMember requireMember(Long familyId, Long memberId) {
        FamilyMember member = familyMemberMapper.selectById(memberId);
        if (member == null || !member.getFamilyId().equals(familyId)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "成员不存在");
        }
        return member;
    }

    private void applyDto(FamilyMember member, FamilyMemberDTO dto) {
        member.setName(dto.getName());
        member.setRelation(dto.getRelation());
        member.setGender(dto.getGender() == null ? 0 : dto.getGender());
        member.setBirthDate(dto.getBirthDate());
        member.setPhone(dto.getPhone());
        member.setAvatarFileId(dto.getAvatarFileId());
        member.setLinkedMemberId(dto.getLinkedMemberId());
        member.setRemark(dto.getRemark());
    }

    private FamilyVO toFamilyVO(Family family) {
        FamilyVO vo = new FamilyVO();
        BeanUtils.copyProperties(family, vo);
        return vo;
    }

    private FamilyMemberVO toVO(FamilyMember member) {
        FamilyMemberVO vo = new FamilyMemberVO();
        BeanUtils.copyProperties(member, vo);
        return vo;
    }

    private FamilyMemberSnapshotDTO toSnapshot(FamilyMember member) {
        FamilyMemberSnapshotDTO dto = new FamilyMemberSnapshotDTO();
        BeanUtils.copyProperties(member, dto);
        return dto;
    }
}
