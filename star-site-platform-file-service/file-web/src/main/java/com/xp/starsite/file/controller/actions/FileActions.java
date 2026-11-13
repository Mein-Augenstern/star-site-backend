package com.xp.starsite.file.controller.actions;

/**
 * 文件域动作名常量（模板留痕 action 字段，规范：服务.资源.动作）
 * <p>
 * 动作名会进模板留痕与 digest，只能新增、不能改名：字符串值与历史值逐字节一致。
 * 常量名取「资源.动作」段，省略服务前缀 file.。
 *
 * @author muyi
 * @since 2026-10
 */
public final class FileActions {

    // —— 文件 ——

    /** 上传文件 */
    public static final String UPLOAD = "file.upload";

    // —— 服务间内部接口 ——

    /** 服务间取文件内容 */
    public static final String INTERNAL_CONTENT = "file.internal.content";

    private FileActions() {
    }
}
