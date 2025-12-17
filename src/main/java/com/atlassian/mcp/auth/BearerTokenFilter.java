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
        
        // 1. 먼저 jira_token, confluence_token 헤더 확인
        String jiraToken = request.getHeader("jira_token");
        String confluenceToken = request.getHeader("confluence_token");
        
        // 2. 없으면 Authorization Bearer 토큰 사용
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        String defaultToken = null;
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            defaultToken = authHeader.substring(BEARER_PREFIX.length()).trim();
        }
        
        // 3. 각 토큰 설정 (우선순위: 개별 헤더 > Authorization)
        String finalJiraToken = (jiraToken != null && !jiraToken.isBlank()) ? jiraToken : defaultToken;
        String finalConfluenceToken = (confluenceToken != null && !confluenceToken.isBlank()) ? confluenceToken : defaultToken;
        
        if (finalJiraToken != null) {
            request.setAttribute("jira_token", finalJiraToken);
            log.debug("Jira token extracted");
        }
        
        if (finalConfluenceToken != null) {
            request.setAttribute("confluence_token", finalConfluenceToken);
            log.debug("Confluence token extracted");
        }
        
        request.setAttribute("auth_mode", "none");
        
        filterChain.doFilter(request, response);
    }
}
