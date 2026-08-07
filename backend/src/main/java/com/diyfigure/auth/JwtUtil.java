package com.diyfigure.auth;

import com.diyfigure.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 负责 token 的生成、解析、校验
 *
 * token payload 中存储:
 * - sub: userId
 * - role: 用户角色(USER / ADMIN)
 * - iat: 签发时间
 * - exp: 过期时间
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT token
     *
     * @param userId 用户 ID
     * @param role   用户角色
     * @return JWT token 字符串
     */
    public String generateToken(Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId.toString());
        claims.put("role", role);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 token,返回 Claims
     *
     * @param token JWT token
     * @return Claims 对象
     * @throws io.jsonwebtoken.JwtException token 无效或已过期时抛出
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 token 中提取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从 token 中提取用户角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 校验 token 是否有效
     *
     * @param token JWT token
     * @return true=有效, false=无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.debug("JWT 校验失败: {}", e.getMessage());
            return false;
        }
    }
}
