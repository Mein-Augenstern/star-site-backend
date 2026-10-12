package com.xp.starsite.member.controller.actions;

/**
 * 会员域动作名常量（模板留痕 action 字段，规范：服务.资源.动作）
 * <p>
 * 动作名会进模板留痕与 digest，只能新增、不能改名：字符串值与历史值逐字节一致。
 * 常量名取「资源.动作」段，省略服务前缀 member.。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class MemberActions {

    // —— 认证 ——

    /** 账号注册 */
    public static final String AUTH_REGISTER = "member.auth.register";

    /** 账密登录 */
    public static final String AUTH_LOGIN = "member.auth.login";

    /** 微信小程序登录（静默注册/登录） */
    public static final String AUTH_WX_LOGIN = "member.auth.wxLogin";

    // —— 登录方式绑定 ——

    /** 账密绑定微信身份 */
    public static final String BIND_ACCOUNT = "member.bind.account";

    /** 我的登录方式列表 */
    public static final String BIND_LIST = "member.bind.list";

    /** 解绑登录方式 */
    public static final String BIND_UNBIND = "member.bind.unbind";

    /** 发送邮箱验证码 */
    public static final String BIND_EMAIL_CODE_SEND = "member.bind.emailCode.send";

    /** 邮箱验证码绑定 */
    public static final String BIND_EMAIL = "member.bind.email";

    // —— 会员资料 ——

    /** 我的资料 */
    public static final String PROFILE_GET = "member.profile.get";

    /** 修改我的资料 */
    public static final String PROFILE_UPDATE = "member.profile.update";

    /** 修改登录密码 */
    public static final String PROFILE_CHANGE_PASSWORD =
            "member.profile.changePassword";

    /** 完善昵称/头像 */
    public static final String PROFILE_COMPLETE = "member.profile.complete";

    // —— 管理端异步管控 ——

    /** star-async 监控总览 */
    public static final String ADMIN_ASYNC_SUMMARY = "member.admin.async.summary";

    /** star-async 分页列表 */
    public static final String ADMIN_ASYNC_LIST = "member.admin.async.list";

    /** star-async 手动重试 */
    public static final String ADMIN_ASYNC_RETRY = "member.admin.async.retry";

    /** star-async 重占（卡单处理） */
    public static final String ADMIN_ASYNC_RECLAIM = "member.admin.async.reclaim";

    /** star-async 终止（置死信） */
    public static final String ADMIN_ASYNC_KILL = "member.admin.async.kill";

    // —— 服务间内部接口 ——

    /** 会员联系方式 */
    public static final String INTERNAL_CONTACT = "member.internal.contact";

    private MemberActions() {
    }
}
