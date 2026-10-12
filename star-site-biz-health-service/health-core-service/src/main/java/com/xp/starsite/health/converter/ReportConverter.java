package com.xp.starsite.health.converter;

import com.xp.starsite.health.dal.dataobject.HealthReportDO;
import com.xp.starsite.health.entity.HealthReport;
import org.springframework.beans.BeanUtils;

/**
 * 体检报告模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class ReportConverter {

    private ReportConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static HealthReportDO toDO(HealthReport report) {
        HealthReportDO row = new HealthReportDO();
        row.setId(report.getId());
        row.setFamilyMemberId(report.getFamilyMemberId());
        row.setReportYear(report.getReportYear());
        row.setCheckupDate(report.getCheckupDate());
        row.setHospital(report.getHospital());
        row.setTitle(report.getTitle());
        row.setSummary(report.getSummary());
        row.setFileId(report.getFileId());
        row.setRemark(report.getRemark());
        row.setInterpretation(report.getInterpretation());
        row.setInterpretedAt(report.getInterpretedAt());
        row.setDeleted(report.getDeleted());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static HealthReport toEntity(HealthReportDO row) {
        HealthReport report = new HealthReport();
        BeanUtils.copyProperties(row, report);
        return report;
    }
}
