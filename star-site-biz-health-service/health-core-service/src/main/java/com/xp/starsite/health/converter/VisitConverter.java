package com.xp.starsite.health.converter;

import com.xp.starsite.health.dal.dataobject.VisitRecordDO;
import com.xp.starsite.health.entity.VisitRecord;
import org.springframework.beans.BeanUtils;

/**
 * 就医记录模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
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
}
