package com.xp.starsite.archive.controller.actions;

/**
 * 档案域动作名常量（模板留痕 action 字段，规范：服务.资源.动作）
 * <p>
 * 动作名会进模板留痕与 digest，只能新增、不能改名：字符串值与历史值逐字节一致。
 * 常量名取「资源.动作」段，省略服务前缀 archive.。
 *
 * @author muyi
 * @since 2026-10
 */
public final class ArchiveActions {

    // —— 证件 ——

    /** 证件创建 */
    public static final String DOCUMENT_CREATE = "archive.document.create";

    /** 证件分页 */
    public static final String DOCUMENT_PAGE = "archive.document.page";

    /** 证件详情 */
    public static final String DOCUMENT_DETAIL = "archive.document.detail";

    /** 证件更新 */
    public static final String DOCUMENT_UPDATE = "archive.document.update";

    /** 证件删除 */
    public static final String DOCUMENT_DELETE = "archive.document.delete";

    // —— 家庭资产 ——

    /** 资产创建 */
    public static final String ASSET_CREATE = "archive.asset.create";

    /** 资产分页 */
    public static final String ASSET_PAGE = "archive.asset.page";

    /** 资产总览（总估值 + 类型分布） */
    public static final String ASSET_SUMMARY = "archive.asset.summary";

    /** 资产详情 */
    public static final String ASSET_DETAIL = "archive.asset.detail";

    /** 资产估值历史 */
    public static final String ASSET_VALUATIONS = "archive.asset.valuations";

    /** 资产更新 */
    public static final String ASSET_UPDATE = "archive.asset.update";

    /** 资产删除 */
    public static final String ASSET_DELETE = "archive.asset.delete";

    // —— 服务间内部接口 ——

    /** 资产总览（供下游服务间调用） */
    public static final String INTERNAL_SUMMARY = "archive.internal.summary";

    private ArchiveActions() {
    }
}
