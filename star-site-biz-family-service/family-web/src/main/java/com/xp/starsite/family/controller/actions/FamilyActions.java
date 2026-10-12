package com.xp.starsite.family.controller.actions;

/**
 * 家庭域动作名常量（模板留痕 action 字段，规范：服务.资源.动作）
 * <p>
 * 动作名会进模板留痕与 digest，只能新增、不能改名：字符串值与历史值逐字节一致。
 * 常量名取「资源.动作」段，省略服务前缀 family.。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class FamilyActions {

    // —— 家庭 ——

    /** 创建家庭 */
    public static final String CREATE = "family.create";

    /** 我的家庭 */
    public static final String MINE = "family.mine";

    // —— 家庭成员 ——

    /** 新增成员档案 */
    public static final String MEMBER_ADD = "family.member.add";

    /** 成员档案列表 */
    public static final String MEMBER_LIST = "family.member.list";

    /** 更新成员档案 */
    public static final String MEMBER_UPDATE = "family.member.update";

    /** 删除成员档案 */
    public static final String MEMBER_DELETE = "family.member.delete";

    // —— 服务间内部接口 ——

    /** 归属校验 */
    public static final String INTERNAL_OWNERSHIP = "family.internal.ownership";

    /** 成员快照全集 */
    public static final String INTERNAL_MEMBERS = "family.internal.members";

    private FamilyActions() {
    }
}
