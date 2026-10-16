package com.xp.starsite.health.service.impl;

import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.health.dto.VisitCreateDTO;
import com.xp.starsite.health.entity.VisitRecord;
import com.xp.starsite.health.repository.VisitRecordRepository;
import com.xp.starsite.health.service.VisitService;
import com.xp.starsite.health.vo.PageVO;
import com.xp.starsite.health.vo.VisitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;

/**
 * 就医记录服务实现。越权防线约定见 {@link com.xp.starsite.health.service.VisitService}。
 * 数据访问经仓储层（{@link VisitRecordRepository}，实现位于 core-service、底层调用 dal DAO），
 * service 只面向领域模型 Entity 编排；VO 映射属于本层（common-biz 出参契约）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitServiceImpl implements VisitService {

    private final VisitRecordRepository visitRepository;
    private final FamilyOwnershipGuard ownershipGuard;

    @Override
    @Transactional(rollbackFor = Exception.class)
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
        VisitRecord saved = visitRepository.save(visit);
        log.info("新建就医记录: id={}, member={}, date={}", saved.getId(), dto.getFamilyMemberId(), dto.getVisitDate());
        return toVO(saved);
    }

    @Override
    public PageVO<VisitVO> listVisits(Long memberId, Long familyMemberId, long page, long size) {
        Set<Long> scope = ownedScope(memberId, familyMemberId);
        if (scope.isEmpty()) {
            return PageVO.of(List.of(), 0, page, size);
        }
        long total = visitRepository.countByScope(scope);
        if (total == 0) {
            return PageVO.of(List.of(), 0, page, size);
        }
        long offset = (page - 1) * size;
        List<VisitVO> vos = visitRepository.pageByScope(scope, offset, (int) size)
                .stream().map(VisitServiceImpl::toVO).toList();
        return PageVO.of(vos, total, page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVisit(Long memberId, Long id) {
        VisitRecord visit = visitRepository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.BAD_REQUEST.getCode(), "就医记录不存在"));
        ownershipGuard.requireOwned(memberId, visit.getFamilyMemberId());
        visitRepository.markDeleted(id);
    }

    /** 领域模型 → 接入层视图 */
    private static VisitVO toVO(VisitRecord v) {
        VisitVO vo = new VisitVO();
        BeanUtils.copyProperties(v, vo);
        return vo;
    }

    /** 列表/搜索的数据范围（指定成员单查，否则全家收口），与报告服务同语义 */
    private Set<Long> ownedScope(Long memberId, Long familyMemberId) {
        if (familyMemberId != null) {
            ownershipGuard.requireOwned(memberId, familyMemberId);
            return Set.of(familyMemberId);
        }
        return Set.copyOf(ownershipGuard.listOwnedMemberIds(memberId));
    }
}
