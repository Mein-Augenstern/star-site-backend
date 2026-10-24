package com.xp.starsite.payment.converter;

import com.xp.starsite.payment.dal.dataobject.PaymentOrderDO;
import com.xp.starsite.payment.entity.PaymentOrder;
import org.springframework.beans.BeanUtils;

/**
 * 支付单模型转换：DO（DAL 表映射）↔ Entity（core-model 领域模型）。
 * 转换只在本层（core-service）发生——dal 不感知领域模型，core-model 不感知表结构。
 * VO（接入层视图）映射不属于本层，由 common-biz 的用例层完成。
 *
 * @author muyi
 * @since 2026-10
 */
public final class PaymentOrderConverter {

    private PaymentOrderConverter() {
    }

    /** 领域模型 → 落库对象 */
    public static PaymentOrderDO toDO(PaymentOrder entity) {
        PaymentOrderDO row = new PaymentOrderDO();
        BeanUtils.copyProperties(entity, row);
        return row;
    }

    /** 落库对象 → 领域模型 */
    public static PaymentOrder toEntity(PaymentOrderDO row) {
        PaymentOrder entity = new PaymentOrder();
        BeanUtils.copyProperties(row, entity);
        return entity;
    }
}
