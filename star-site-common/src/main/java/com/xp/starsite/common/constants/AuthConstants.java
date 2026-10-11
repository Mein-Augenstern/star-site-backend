package com.xp.starsite.common.constants;

/**
 * 认证相关常量
 *
 * <p>全平台认证链路共用的 HTTP 头/Token 前缀/JWT claim 名约定，
 * 供 JwtUtil 签发校验、member 服务登录签发、网关与各服务拦截器鉴权时对齐使用。
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class AuthConstants {

    private AuthConstants() {
    }

    /** JWT 请求头前缀 */
    public static final String AUTH_HEADER = "Authorization";

    /** Token 前缀 */
    public static final String BEARER_PREFIX = "Bearer ";

    /** JWT 中用户 ID 的 claim 名 */
    public static final String CLAIM_MEMBER_ID = "memberId";

    /** JWT 中用户名的 claim 名 */
    public static final String CLAIM_USERNAME = "username";
}
