package com.xp.starsite.insurance.converter;

import com.xp.starsite.insurance.dal.dataobject.InsurancePolicyDO;
import com.xp.starsite.insurance.entity.InsurancePolicy;
import org.springframework.beans.BeanUtils;

/**
 * 保单模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class InsurancePolicyConverter {

    private InsurancePolicyConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static InsurancePolicyDO toDO(InsurancePolicy policy) {
        InsurancePolicyDO row = new InsurancePolicyDO();
        row.setId(policy.getId());
        row.setFamilyId(policy.getFamilyId());
        row.setOwnerMemberId(policy.getOwnerMemberId());
        row.setCompany(policy.getCompany());
        row.setProductName(policy.getProductName());
        row.setPolicyNo(policy.getPolicyNo());
        row.setCategory(policy.getCategory());
        row.setInsuredMemberId(policy.getInsuredMemberId());
        row.setInsuredMemberName(policy.getInsuredMemberName());
        row.setStartDate(policy.getStartDate());
        row.setMaturityDate(policy.getMaturityDate());
        row.setPremium(policy.getPremium());
        row.setPaymentFreq(policy.getPaymentFreq());
        row.setNextPaymentDate(policy.getNextPaymentDate());
        row.setCoverageAmount(policy.getCoverageAmount());
        row.setStatus(policy.getStatus());
        row.setFileId(policy.getFileId());
        row.setRemark(policy.getRemark());
        row.setDeleted(policy.getDeleted());
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static InsurancePolicy toEntity(InsurancePolicyDO row) {
        InsurancePolicy policy = new InsurancePolicy();
        BeanUtils.copyProperties(row, policy);
        return policy;
    }
}
