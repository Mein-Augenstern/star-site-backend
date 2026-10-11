package com.xp.starsite.archive.service.impl;

import com.xp.starsite.api.client.FamilyOwnershipGuard;
import com.xp.starsite.api.dto.FamilyMemberSnapshotDTO;
import com.xp.starsite.api.event.reminder.ReminderCancelEvent;
import com.xp.starsite.api.event.reminder.ReminderRegisterEvent;
import com.xp.starsite.api.event.reminder.ReminderRepeatType;
import com.xp.starsite.archive.dto.AssetSaveDTO;
import com.xp.starsite.archive.entity.ArchiveAsset;
import com.xp.starsite.archive.entity.AssetValuation;
import com.xp.starsite.archive.mapper.ArchiveAssetMapper;
import com.xp.starsite.archive.mapper.AssetValuationMapper;
import com.xp.starsite.archive.projection.AssetTypeSummary;
import com.xp.starsite.archive.service.AssetService;
import com.xp.starsite.archive.vo.AssetSummaryVO;
import com.xp.starsite.archive.vo.AssetVO;
import com.xp.starsite.archive.vo.PageVO;
import com.xp.starsite.archive.vo.ValuationPointVO;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.mq.EventPublisher;
import com.xp.starsite.common.mq.event.EventTypes;
import com.xp.starsite.common.mq.event.Topics;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.common.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 家庭资产实现。估值手动更新（季度节奏），每次变更自动追加历史留痕。
 * 车辆年检是资产域唯一提醒联动：年检日期非空 → ANNUAL 提醒（事务消息）。
 * <p>
 * 数据访问全部经 DAL（{@link ArchiveAssetMapper}/{@link AssetValuationMapper}）：
 * SQL 收敛在 mapper XML，业务层零 SQL 拼装；逻辑删（deleted=0 过滤 / UPDATE 置位）。
 * 主键为应用侧雪花 id（{@link IdGenerator}）——半消息载荷需在 insert 前拿到 id，
 * 该语义保持不变。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private static final Set<String> ASSET_TYPES = Set.of(
            "real_estate", "vehicle", "deposit", "investment", "fund", "other");
    private static final List<String> TYPE_ORDER = List.of(
            "real_estate", "vehicle", "deposit", "investment", "fund", "other");

    private static final int INSPECT_ADVANCE_DAYS = 15;

    private final ArchiveAssetMapper assetMapper;
    private final AssetValuationMapper valuationMapper;
    private final FamilyOwnershipGuard ownershipGuard;
    private final EventPublisher eventPublisher;
    /** 雪花 id（替代 MyBatis-Plus ASSIGN_ID）：事务消息载荷需 insert 前拿到 id */
    private final IdGenerator idGenerator;

    @Override
    public AssetVO create(Long memberId, AssetSaveDTO dto) {
        FamilyMemberSnapshotDTO holder = resolveHolder(memberId, dto.getHolderMemberId());
        ArchiveAsset asset = new ArchiveAsset();
        // 雪花 id 应用侧先赋值：年检提醒半消息载荷需要 bizId
        asset.setId(idGenerator.nextId());
        applyDto(asset, dto);
        asset.setFamilyId(holder != null ? holder.getFamilyId() : requireFamilyId(memberId));
        asset.setOwnerMemberId(memberId);
        asset.setHolderMemberName(holder != null ? holder.getName() : null);
        // 首次估值留痕
        if (asset.getCurrentValue() != null) {
            asset.setValuedAt(LocalDateTime.now());
        }
        if (asset.getNextInspectDate() != null) {
            // 年检提醒与落库原子：insert 放事务回调，半消息先发
            eventPublisher.publishTransactional(Topics.TOPIC_REMINDER, EventTypes.REMINDER_REGISTER,
                    "asset", asset.getId(), inspectReminderOf(asset), () -> assetMapper.insert(asset));
        } else {
            assetMapper.insert(asset);
        }
        insertValuation(asset);
        log.info("创建资产: id={}, name={}, type={}", asset.getId(), asset.getName(), asset.getAssetType());
        return toVO(assetMapper.selectById(asset.getId()));
    }

    @Override
    public PageVO<AssetVO> list(Long memberId, String assetType, long page, long size) {
        // 分页 count 先行：0 直接短路，不再取页
        long total = assetMapper.countByOwner(memberId, StringUtils.hasText(assetType) ? assetType : null);
        if (total == 0) {
            return PageVO.of(List.of(), 0, page, size);
        }
        long offset = (page - 1) * size;
        List<AssetVO> vos = assetMapper
                .selectPage(memberId, StringUtils.hasText(assetType) ? assetType : null, offset, (int) size)
                .stream().map(this::toVO).toList();
        return PageVO.of(vos, total, page, size);
    }

    @Override
    public AssetVO getDetail(Long memberId, Long id) {
        return toVO(requireOwnedAsset(memberId, id));
    }

    @Override
    @Transactional
    public AssetVO update(Long memberId, Long id, AssetSaveDTO dto) {
        ArchiveAsset asset = requireOwnedAsset(memberId, id);
        FamilyMemberSnapshotDTO holder = resolveHolder(memberId, dto.getHolderMemberId());
        BigDecimal oldValue = asset.getCurrentValue();
        applyDto(asset, dto);
        asset.setHolderMemberName(holder != null ? holder.getName() : null);
        // 估值变更：刷新估值时间 + 追加历史
        if (dto.getCurrentValue() != null && (oldValue == null || dto.getCurrentValue().compareTo(oldValue) != 0)) {
            asset.setValuedAt(LocalDateTime.now());
        }
        assetMapper.update(asset);
        if (asset.getValuedAt() != null && (oldValue == null
                || (asset.getCurrentValue() != null && asset.getCurrentValue().compareTo(oldValue) != 0))) {
            insertValuation(asset);
        }
        // 年检日变更：非空 upsert 规则，清空撤销
        if (asset.getNextInspectDate() != null) {
            eventPublisher.publish(Topics.TOPIC_REMINDER, EventTypes.REMINDER_REGISTER,
                    "asset", asset.getId(), inspectReminderOf(asset));
        } else {
            publishCancel(asset);
        }
        return toVO(assetMapper.selectById(id));
    }

    @Override
    public void delete(Long memberId, Long id) {
        ArchiveAsset asset = requireOwnedAsset(memberId, id);
        assetMapper.markDeleted(id);
        publishCancel(asset);
        log.info("删除资产: id={}", id);
    }

    @Override
    public List<ValuationPointVO> valuations(Long memberId, Long assetId) {
        requireOwnedAsset(memberId, assetId);
        return valuationMapper.selectByAssetId(assetId)
                .stream().map(v -> new ValuationPointVO(v.getValue(), v.getValuedAt())).toList();
    }

    @Override
    public AssetSummaryVO summary(Long memberId) {
        // 总估值与类型分组都在 DAL 聚合（SUM/GROUP BY），业务层只做类型排序
        AssetSummaryVO vo = new AssetSummaryVO();
        vo.setTotalValue(assetMapper.selectTotalValue(memberId));
        Map<String, AssetTypeSummary> byType = assetMapper.selectTypeSummaries(memberId)
                .stream().collect(Collectors.toMap(AssetTypeSummary::getAssetType, Function.identity()));
        List<AssetSummaryVO.TypeSlice> slices = new ArrayList<>();
        for (String type : TYPE_ORDER) {
            AssetTypeSummary row = byType.get(type);
            if (row == null || row.getCount() == null || row.getCount() == 0) {
                continue;
            }
            AssetSummaryVO.TypeSlice slice = new AssetSummaryVO.TypeSlice();
            slice.setAssetType(row.getAssetType());
            slice.setTotalValue(row.getTotalValue());
            slice.setCount(row.getCount().intValue());
            slices.add(slice);
        }
        vo.setSlices(slices);
        return vo;
    }

    private ArchiveAsset requireOwnedAsset(Long memberId, Long id) {
        ArchiveAsset asset = assetMapper.selectById(id);
        if (asset == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "资产不存在");
        }
        if (!asset.getOwnerMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return asset;
    }

    /** 持有人可空（家庭共有）：传了才做归属校验 */
    private FamilyMemberSnapshotDTO resolveHolder(Long memberId, Long holderMemberId) {
        return holderMemberId == null ? null : ownershipGuard.requireOwned(memberId, holderMemberId);
    }

    /** 家庭共有资产没有 holder 快照可取 familyId，单独查一次 */
    private Long requireFamilyId(Long memberId) {
        return ownershipGuard.listOwnedMembers(memberId).stream()
                .findFirst()
                .map(FamilyMemberSnapshotDTO::getFamilyId)
                .orElseThrow(() -> new BizException(ResultCode.BAD_REQUEST.getCode(), "请先创建家庭"));
    }

    private void insertValuation(ArchiveAsset asset) {
        if (asset.getCurrentValue() == null) {
            return;
        }
        AssetValuation v = new AssetValuation();
        v.setAssetId(asset.getId());
        v.setValue(asset.getCurrentValue());
        v.setValuedAt(asset.getValuedAt() == null ? LocalDateTime.now() : asset.getValuedAt());
        valuationMapper.insert(v);
    }

    /** 年检提醒：年检日前 15 天，逐年重复（车辆一直开就一直检） */
    private ReminderRegisterEvent inspectReminderOf(ArchiveAsset asset) {
        ReminderRegisterEvent event = new ReminderRegisterEvent();
        event.setSource("star-site-archive");
        event.setBizType("vehicle_inspect");
        event.setBizId(asset.getId());
        event.setFamilyId(asset.getFamilyId());
        event.setOwnerMemberId(asset.getOwnerMemberId());
        event.setTitle(asset.getName() + " 年检");
        event.setFirstRemindAt(asset.getNextInspectDate() == null
                ? null : asset.getNextInspectDate().minusDays(INSPECT_ADVANCE_DAYS));
        event.setRepeatType(ReminderRepeatType.ANNUAL);
        event.setRepeatUntil(null);
        event.setAdvanceDays(INSPECT_ADVANCE_DAYS);
        return event;
    }

    private void publishCancel(ArchiveAsset asset) {
        ReminderCancelEvent event = new ReminderCancelEvent();
        event.setSource("star-site-archive");
        event.setBizType("vehicle_inspect");
        event.setBizId(asset.getId());
        eventPublisher.publish(Topics.TOPIC_REMINDER, EventTypes.REMINDER_CANCEL, "asset", asset.getId(), event);
    }

    private void applyDto(ArchiveAsset asset, AssetSaveDTO dto) {
        if (!ASSET_TYPES.contains(dto.getAssetType())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "不支持的资产类型");
        }
        asset.setName(dto.getName());
        asset.setAssetType(dto.getAssetType());
        asset.setHolderMemberId(dto.getHolderMemberId());
        asset.setAcquireDate(dto.getAcquireDate());
        asset.setCurrentValue(dto.getCurrentValue());
        asset.setNextInspectDate(dto.getNextInspectDate());
        asset.setRemark(dto.getRemark());
    }

    private AssetVO toVO(ArchiveAsset asset) {
        AssetVO vo = new AssetVO();
        BeanUtils.copyProperties(asset, vo);
        return vo;
    }
}
