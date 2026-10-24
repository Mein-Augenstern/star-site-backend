package com.xp.starsite.family.service;

import com.xp.starsite.api.dto.FamilyMemberSnapshotDTO;
import com.xp.starsite.family.dto.FamilyCreateDTO;
import com.xp.starsite.family.dto.FamilyMemberDTO;
import com.xp.starsite.family.vo.FamilyMemberVO;
import com.xp.starsite.family.vo.FamilyVO;

import java.util.List;

/**
 * 家庭档案服务
 *
 * @author muyi
 * @since 2026-10
 */
public interface FamilyService {

    /**
     * 创建家庭（一个账号只能有一个家庭）
     *
     * @param ownerMemberId 户主账号 id
     * @param dto           家庭信息（名称/备注）
     * @return 新建家庭视图
     * @throws com.xp.starsite.common.exception.BizException 该账号已有家庭（400）
     */
    FamilyVO create(Long ownerMemberId, FamilyCreateDTO dto);

    /**
     * 我的家庭
     *
     * @param ownerMemberId 户主账号 id
     * @return 家庭视图，没有则为 null
     */
    FamilyVO mine(Long ownerMemberId);

    /**
     * 添加成员档案
     *
     * @param ownerMemberId 户主账号 id
     * @param dto           成员档案内容
     * @return 新建成员视图
     * @throws com.xp.starsite.common.exception.BizException 尚未创建家庭（400）
     */
    FamilyMemberVO addMember(Long ownerMemberId, FamilyMemberDTO dto);

    /**
     * 成员列表
     *
     * @param ownerMemberId 户主账号 id
     * @return 该家庭全部成员视图
     * @throws com.xp.starsite.common.exception.BizException 尚未创建家庭（400）
     */
    List<FamilyMemberVO> listMembers(Long ownerMemberId);

    /**
     * 编辑成员档案
     *
     * @param ownerMemberId 户主账号 id
     * @param memberId      成员档案 id
     * @param dto           成员档案内容（非空字段才更新）
     * @return 更新后的成员视图
     * @throws com.xp.starsite.common.exception.BizException 尚未创建家庭（400）或成员不存在/不属于该家庭（400）
     */
    FamilyMemberVO updateMember(Long ownerMemberId, Long memberId, FamilyMemberDTO dto);

    /**
     * 删除成员档案（逻辑删）
     *
     * @param ownerMemberId 户主账号 id
     * @param memberId      成员档案 id
     * @throws com.xp.starsite.common.exception.BizException 尚未创建家庭（400）或成员不存在/不属于该家庭（400）
     */
    void deleteMember(Long ownerMemberId, Long memberId);

    /**
     * 归属校验（内部接口用）：familyMemberId 属于该账号的家庭则返回快照，否则 null。
     * 其他服务（health 等）经 FamilyOwnershipGuard 调用，做越权防线。
     *
     * @param ownerMemberId  户主账号 id
     * @param familyMemberId 待校验的成员档案 id
     * @return 成员快照；账号无家庭或成员不属于该家庭时为 null
     */
    FamilyMemberSnapshotDTO checkOwnership(Long ownerMemberId, Long familyMemberId);

    /**
     * 账号家庭的全量成员快照（内部接口用）
     *
     * @param ownerMemberId 户主账号 id
     * @return 成员快照列表（无家庭返回空列表）
     */
    List<FamilyMemberSnapshotDTO> listMemberSnapshots(Long ownerMemberId);
}
