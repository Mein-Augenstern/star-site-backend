package com.xiaopu.starsite.common.constants;

/**
 * 认证相关常量
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
