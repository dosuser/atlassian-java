package com.atlassian.mcp.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 검증 서비스.
 * security.mode=jwt일 때만 활성화됨.
 * 
 * 지원하는 형식:
 * 1. JWS (JSON Web Signature) - 서명된 JWT
 *    - HS256 (HMAC-SHA256): 최소 32바이트 secret
 *    - HS384 (HMAC-SHA384): 최소 48바이트 secret
 *    - HS512 (HMAC-SHA512): 최소 64바이트 secret
 * 
 * 2. JWE (JSON Web Encryption) - 암호화된 JWT
 *    - A128GCM, A192GCM, A256GCM (AES-GCM)
 *    - A128CBC-HS256, A192CBC-HS384, A256CBC-HS512 (AES-CBC + HMAC)
 * 
 * JWT 헤더의 'alg'/'enc' 필드에 따라 자동으로 알고리즘 선택
 */
@Service
@ConditionalOnProperty(name = "app.security.mode", havingValue = "jwt")
public class JwtService {
    
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final SecretKey secretKey;
    
    public JwtService(@Value("${app.jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is required when security.mode=jwt. Set JWT_SECRET environment variable.");
        }
        
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        
        // Secret 길이 검증 (최소 32바이트)
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                String.format("JWT secret must be at least 32 bytes. Current: %d bytes. " +
                    "Recommended: 32+ bytes for HS256/A128, 48+ bytes for HS384/A192, 64+ bytes for HS512/A256", 
                    secretBytes.length)
            );
        }
        
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
        log.info("JWT Service initialized with secret key (length: {} bytes)", secretBytes.length);
    }
    
    /**
     * JWT 토큰 검증 및 Claims 추출 (JWS 또는 JWE 자동 감지).
     * 
     * JWT 타입 자동 감지:
     * - JWS (서명): 3개 파트 (header.payload.signature)
     * - JWE (암호화): 5개 파트 (header.encryptedKey.iv.ciphertext.tag)
     * 
     * @param token JWT 토큰
     * @return Claims
     * @throws io.jsonwebtoken.JwtException JWT 검증 실패 (서명 불일치, 만료, 형식 오류)
     */
    public Claims validateAndGetClaims(String token) {
        int dotCount = token.length() - token.replace(".", "").length();
        
        try {
            if (dotCount == 4) {
                // JWE (암호화된 JWT) - 5개 파트
                log.debug("Detected JWE token (encrypted)");
                var jwe = Jwts.parser()
                        .decryptWith(secretKey)
                        .build()
                        .parseEncryptedClaims(token);
                
                String algorithm = jwe.getHeader().getAlgorithm();
                String encryption = (String) jwe.getHeader().get("enc");
                log.info("JWT validated successfully - JWE with alg={}, enc={}", algorithm, encryption);
                
                return jwe.getPayload();
                
            } else if (dotCount == 2) {
                // JWS (서명된 JWT) - 3개 파트
                log.debug("Detected JWS token (signed)");
                var jws = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token);
                
                String algorithm = jws.getHeader().getAlgorithm();
                log.info("JWT validated successfully - JWS with alg={}", algorithm);
                
                return jws.getPayload();
                
            } else {
                throw new JwtException("Invalid JWT format: expected 3 parts (JWS) or 5 parts (JWE), got " + (dotCount + 1));
            }
        } catch (JwtException e) {
            log.error("JWT validation failed: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * JWT에서 subject (user ID) 추출.
     */
    public String getUserId(Claims claims) {
        return claims.getSubject();
    }
}
