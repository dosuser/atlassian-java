package com.atlassian.mcp.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 검증 서비스.
 * security.mode=jwt일 때만 활성화됨.
 */
@Service
@ConditionalOnProperty(name = "app.security.mode", havingValue = "jwt")
public class JwtService {
    
    private final SecretKey secretKey;
    
    public JwtService(@Value("${app.jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is required when security.mode=jwt. Set JWT_SECRET environment variable.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * JWT 토큰 검증 및 Claims 추출.
     * 
     * @param token JWT 토큰
     * @return Claims
     * @throws io.jsonwebtoken.JwtException JWT 검증 실패
     */
    public Claims validateAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * JWT에서 subject (user ID) 추출.
     */
    public String getUserId(Claims claims) {
        return claims.getSubject();
    }
}
