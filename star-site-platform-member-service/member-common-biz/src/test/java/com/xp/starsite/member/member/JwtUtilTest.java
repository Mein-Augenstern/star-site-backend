package com.xp.starsite.member;

import com.xp.starsite.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JwtUtil 单测：验证 token 签发/解析往返、非法与过期 token 均被拒绝（认证防线，不启 Spring 上下文）
 *
 * @author muyi
 * @since 2026-10
 */
class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-key-must-be-32-bytes!!";

    @Test
    void generateAndParse() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 720);
        String token = jwtUtil.generateToken(42L, "tom");

        Claims claims = jwtUtil.parseToken(token);
        assertEquals("42", claims.getSubject());
        assertEquals("tom", claims.get("username", String.class));
    }

    @Test
    void illegalTokenThrows() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 720);
        assertThrows(Exception.class, () -> jwtUtil.parseToken("not-a-jwt-token"));
    }

    @Test
    void expiredTokenThrows() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, -1);
        String token = jwtUtil.generateToken(42L, "tom");
        assertThrows(Exception.class, () -> jwtUtil.parseToken(token));
    }
}
