package com.xp.starsite.archive.controller;

import com.xp.starsite.archive.dto.AssetSaveDTO;
import com.xp.starsite.archive.service.AssetService;
import com.xp.starsite.archive.vo.AssetSummaryVO;
import com.xp.starsite.archive.vo.AssetVO;
import com.xp.starsite.archive.vo.PageVO;
import com.xp.starsite.archive.vo.ValuationPointVO;
import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 家庭资产接口（全部需 JWT；memberId 用于归属校验）
 * <p>
 * 业务逻辑统一经模板包裹：模板包掉成功与失败的全部响应处理（成功 R.ok / 失败 R.fail），
 * Controller 一行返回；动作命名规范 archive.asset.*。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/archive/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 创建资产（车辆带年检日 → 发年检提醒事件）
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param dto      资产元数据
     * @return 新建资产视图
     */
    @PostMapping
    public R<AssetVO> create(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                             @Valid @RequestBody AssetSaveDTO dto) {
        return writeTemplate.write("archive.asset.create",
                () -> assetService.create(memberId, dto));
    }

    /**
     * 资产分页列表（筛选：类型）
     *
     * @param memberId  当前登录账号（JWT 注入，用于归属校验）
     * @param assetType 可选，按资产类型过滤
     * @param page      页码（从 1 起）
     * @param size      每页条数
     * @return 资产分页
     */
    @GetMapping
    public R<PageVO<AssetVO>> list(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                   @RequestParam(required = false) String assetType,
                                   @RequestParam(defaultValue = "1") long page,
                                   @RequestParam(defaultValue = "20") long size) {
        return queryTemplate.query("archive.asset.page",
                () -> assetService.list(memberId, assetType, page, size));
    }

    /**
     * 资产总览：总估值 + 按类型分布
     * <p>注意路由顺序：先声明固定路径再声明 {id}。
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @return 资产总览视图
     */
    @GetMapping("/summary")
    public R<AssetSummaryVO> summary(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query("archive.asset.summary",
                () -> assetService.summary(memberId));
    }

    /**
     * 资产详情
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param id       资产 id
     * @return 资产视图
     */
    @GetMapping("/{id}")
    public R<AssetVO> detail(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                             @PathVariable Long id) {
        return queryTemplate.query("archive.asset.detail",
                () -> assetService.getDetail(memberId, id));
    }

    /**
     * 估值历史（曲线数据，按时间升序）
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param id       资产 id
     * @return 估值历史点列表
     */
    @GetMapping("/{id}/valuations")
    public R<List<ValuationPointVO>> valuations(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                                @PathVariable Long id) {
        return queryTemplate.query("archive.asset.valuations",
                () -> assetService.valuations(memberId, id));
    }

    /**
     * 更新资产（估值变更自动追加历史；年检日变更重发/撤销提醒）
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param id       资产 id
     * @param dto      资产元数据（全量覆盖）
     * @return 更新后资产视图
     */
    @PutMapping("/{id}")
    public R<AssetVO> update(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                             @PathVariable Long id,
                             @Valid @RequestBody AssetSaveDTO dto) {
        return writeTemplate.write("archive.asset.update",
                () -> assetService.update(memberId, id, dto));
    }

    /**
     * 删除资产（撤销年检提醒）
     *
     * @param memberId 当前登录账号（JWT 注入，用于归属校验）
     * @param id       资产 id
     * @return 统一成功响应（无数据载荷）
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        return writeTemplate.write("archive.asset.delete",
                () -> assetService.delete(memberId, id));
    }
}
