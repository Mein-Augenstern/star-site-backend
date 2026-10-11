package com.xp.starsite.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.health.dto.VisitCreateDTO;
import com.xp.starsite.health.entity.VisitRecord;
import com.xp.starsite.health.mapper.VisitRecordMapper;
import com.xp.starsite.health.service.VisitService;
import com.xp.starsite.health.vo.PageVO;
import com.xp.starsite.health.vo.VisitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 就医记录服务实现。越权防线约定见 {@link com.xp.starsite.health.service.VisitService}。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitServiceImpl implements VisitService {

    private final VisitRecordMapper visitMapper;
    private final FamilyOwnershipGuard ownershipGuard;

    @Override
    public VisitVO createVisit(Long memberId, VisitCreateDTO dto) {
        ownershipGuard.requireOwned(memberId, dto.getFamilyMemberId());
        VisitRecord visit = new VisitRecord();
        visit.setFamilyMemberId(dto.getFamilyMemberId());
        visit.setVisitDate(dto.getVisitDate());
        visit.setHospital(dto.getHospital());
        visit.setDepartment(dto.getDepartment());
        visit.setDiagnosis(dto.getDiagnosis());
        visit.setMedication(dto.getMedication());
        visit.setCost(dto.getCost());
        visit.setRemark(dto.getRemark());
        visitMapper.insert(visit);
        log.info("新建就医记录: id={}, member={}, date={}", visit.getId(), dto.getFamilyMemberId(), dto.getVisitDate());
        return toVO(visit);
    }

    @Override
    public PageVO<VisitVO> listVisits(Long memberId, Long familyMemberId, long page, long size) {
        Set<Long> scope = ownedScope(memberId, familyMemberId);
        if (scope.isEmpty()) {
            return PageVO.of(List.of(), 0, page, size);
        }
        LambdaQueryWrapper<VisitRecord> wrapper = new LambdaQueryWrapper<VisitRecord>()
                .in(VisitRecord::getFamilyMemberId, scope)
                .orderByDesc(VisitRecord::getVisitDate)
                .orderByDesc(VisitRecord::getId);
        Page<VisitRecord> result = visitMapper.selectPage(Page.of(page, size), wrapper);
        List<VisitVO> vos = result.getRecords().stream().map(this::toVO).toList();
        return PageVO.of(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void deleteVisit(Long memberId, Long id) {
        VisitRecord visit = visitMapper.selectById(id);
        if (visit == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "就医记录不存在");
        }
        ownershipGuard.requireOwned(memberId, visit.getFamilyMemberId());
        visitMapper.deleteById(id);
    }

    /** 列表/搜索的数据范围（指定成员单查，否则全家收口），与报告服务同语义 */
    private Set<Long> ownedScope(Long memberId, Long familyMemberId) {
        if (familyMemberId != null) {
            ownershipGuard.requireOwned(memberId, familyMemberId);
            return Set.of(familyMemberId);
        }
        return Set.copyOf(ownershipGuard.listOwnedMemberIds(memberId));
    }

    private VisitVO toVO(VisitRecord v) {
        VisitVO vo = new VisitVO();
        BeanUtils.copyProperties(v, vo);
        return vo;
    }
}
