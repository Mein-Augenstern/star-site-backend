package com.xp.starsite.health.converter;

import com.xp.starsite.health.dal.dataobject.VisitRecordDO;
import com.xp.starsite.health.entity.VisitRecord;
import com.xp.starsite.health.vo.VisitVO;
import org.springframework.beans.BeanUtils;

/**
 * 就医记录模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）↔ VO（接入层）。
 * 转换只在本层（biz）发生——dal 不感知领域模型，core-model 不感知表结构。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class VisitConverter {

    private VisitConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static VisitRecordDO toDO(VisitRecord visit) {
        VisitRecordDO row = new VisitRecordDO();
        row.setId(visit.getId());
        row.setFamilyMemberId(visit.getFamilyMemberId());
        row.setVisitDate(visit.getVisitDate());
        row.setHospital(visit.getHospital());
        row.setDepartment(visit.getDepartment());
        row.setDiagnosis(visit.getDiagnosis());
        row.setMedication(visit.getMedication());
        row.setCost(visit.getCost());
        row.setRemark(visit.getRemark());
        row.setDeleted(visit.getDeleted());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static VisitRecord toEntity(VisitRecordDO row) {
        VisitRecord visit = new VisitRecord();
        BeanUtils.copyProperties(row, visit);
        return visit;
    }

    /** 领域模型 → 接入层视图 */
    public static VisitVO toVO(VisitRecord visit) {
        VisitVO vo = new VisitVO();
        BeanUtils.copyProperties(visit, vo);
        return vo;
    }
}
