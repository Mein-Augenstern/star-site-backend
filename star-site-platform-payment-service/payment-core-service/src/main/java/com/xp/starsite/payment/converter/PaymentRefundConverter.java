package com.xp.starsite.payment.converter;

import com.xp.starsite.payment.dal.dataobject.PaymentRefundDO;
import com.xp.starsite.payment.entity.PaymentRefund;
import org.springframework.beans.BeanUtils;

/**
 * 退款单模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author muyi
 * @since 2026-10
 */
public final class PaymentRefundConverter {

    private PaymentRefundConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static PaymentRefundDO toDO(PaymentRefund entity) {
        PaymentRefundDO row = new PaymentRefundDO();
        BeanUtils.copyProperties(entity, row);
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static PaymentRefund toEntity(PaymentRefundDO row) {
        PaymentRefund entity = new PaymentRefund();
        BeanUtils.copyProperties(row, entity);
        return entity;
    }
}
