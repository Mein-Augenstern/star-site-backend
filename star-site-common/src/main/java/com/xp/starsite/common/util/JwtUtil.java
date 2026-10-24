package com.xp.starsite.common.util;

import com.xp.starsite.common.constants.AuthConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具（jjwt 0.12.x API）
 *
 * <p>全平台统一的 token 签发/解析入口：member 服务登录成功后 generateToken，
 * 各业务服务的 JwtAuthInterceptor（MVC）与网关 JwtGlobalFilter（WebFlux）用 parseToken 校验，
 * claim 名以 AuthConstants 为准。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
public class JwtUtil {

    private final SecretKey key;

    @Getter
    private final long expireMinutes;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expire-minutes:720}") long expireMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMinutes = expireMinutes;
    }

    /** 签发 token，subject = memberId */
    public String generateToken(Long memberId, String username) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireMinutes * 60_000L);
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(AuthConstants.CLAIM_USERNAME, username)
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验 token，非法或过期抛 io.jsonwebtoken.JwtException / IllegalArgumentException
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
