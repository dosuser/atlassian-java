package com.atlassian.mcp.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터.
 * security.mode=jwt일 때만 활성화됨.
 * 
 * Authorization: Bearer <JWT> → JWT 검증
 * JIRA_TOKEN: <token> → Jira 인증 토큰
 * CONFLUENCE_TOKEN: <token> → Confluence 인증 토큰
 */
@Component
@ConditionalOnProperty(name = "app.security.mode", havingValue = "jwt")
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String JIRA_TOKEN_HEADER = "JIRA_TOKEN";
    private static final String CONFLUENCE_TOKEN_HEADER = "CONFLUENCE_TOKEN";
    
    private final JwtService jwtService;
    
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // 인증 실패는 항상 ERROR 로그
            log.error("Missing or invalid Authorization header");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT token required");
            return;
        }
        
        String jwt = authHeader.substring(BEARER_PREFIX.length()).trim();
        
        try {
            // JWT 검증
            Claims claims = jwtService.validateAndGetClaims(jwt);
            String userId = jwtService.getUserId(claims);
            
            if (userId == null || userId.isBlank()) {
                // 인증 실패는 항상 ERROR 로그
                log.error("JWT missing subject claim");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT: missing subject");
                return;
            }
            
            // Atlassian 토큰은 별도 헤더에서 추출
            String jiraToken = request.getHeader(JIRA_TOKEN_HEADER);
            String confluenceToken = request.getHeader(CONFLUENCE_TOKEN_HEADER);
            
            // Request attributes에 저장
            request.setAttribute("user_id", userId);
            request.setAttribute("jira_token", jiraToken);
            request.setAttribute("confluence_token", confluenceToken);
            request.setAttribute("auth_mode", "jwt");
            
            log.debug("JWT authenticated: userId={}", userId);
            
        } catch (Exception e) {
            // 인증 실패는 항상 ERROR 로그
            log.error("JWT validation failed: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
