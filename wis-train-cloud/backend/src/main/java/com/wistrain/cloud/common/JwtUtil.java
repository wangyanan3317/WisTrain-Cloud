package com.wistrain.cloud.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与解析（NF-06）
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long hours;

    public JwtUtil(@Value("${wistrain.jwt-secret}") String secret,
                   @Value("${wistrain.jwt-hours}") long hours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.hours = hours;
    }

    public String create(Long userId, Integer role, Long deptId, String realname) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("deptId", deptId)
                .claim("name", realname)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + hours * 3600_000L))
                .signWith(key)
                .compact();
    }

    /** 解析失败抛 io.jsonwebtoken.JwtException / IllegalArgumentException */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }
}
