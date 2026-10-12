package com.xp.starsite.health.service;

import com.xp.starsite.health.dto.VisitCreateDTO;
import com.xp.starsite.health.vo.PageVO;
import com.xp.starsite.health.vo.VisitVO;

/**
 * 就医记录服务：门诊/住院/产检等就诊事件的登记、查询与删除。
 * 归属防线约定同 {@link ReportService}（首参 memberId + family 归属校验，fail-closed）。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface VisitService {

    /**
     * 新建就医记录
     *
     * @param memberId 当前登录账号
     * @param dto      就诊信息（家庭成员/日期/医院/科室/诊断/用药/费用等）
     * @return 新建记录视图
     * @throws com.xp.starsite.common.exception.BizException 403 家庭成员不归属当前账号
     */
    VisitVO createVisit(Long memberId, VisitCreateDTO dto);

    /**
     * 就医记录分页列表（按就诊日期倒序）
     *
     * @param memberId       当前登录账号
     * @param familyMemberId 可选，指定成员则单查（校验归属）；不传=全家范围
     * @param page           页码（1 起）
     * @param size           页大小
     * @return 就医记录分页
     * @throws com.xp.starsite.common.exception.BizException 403 家庭成员不归属当前账号
     */
    PageVO<VisitVO> listVisits(Long memberId, Long familyMemberId, long page, long size);

    /**
     * 删除就医记录（逻辑删，先校验归属）
     *
     * @param memberId 当前登录账号
     * @param id       记录 id
     * @throws com.xp.starsite.common.exception.BizException 400 记录不存在；403 非本人家庭成员的记录
     */
    void deleteVisit(Long memberId, Long id);
}
