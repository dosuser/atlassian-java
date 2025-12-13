package com.atlassian.mcp.auth;

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
 * Bearer 토큰 추출 필터.
 * security.mode=none (기본값)일 때만 활성화됨.
 * Python의 request.state.user_atlassian_token과 동일한 역할.
 * 
 * Authorization: Bearer <token> → Jira/Confluence 모두에 사용
 */
@Component
@ConditionalOnProperty(name = "app.security.mode", havingValue = "none", matchIfMissing = true)
public class BearerTokenFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(BearerTokenFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            
            // Python의 request.state.user_atlassian_token과 동일
            // Jira와 Confluence 모두 동일한 토큰 사용
            request.setAttribute("jira_token", token);
            request.setAttribute("confluence_token", token);
            request.setAttribute("auth_mode", "none");
            
            log.debug("Bearer token extracted for Jira and Confluence");
        } else {
            log.debug("No Bearer token in Authorization header");
        }
        
        filterChain.doFilter(request, response);
    }
}
