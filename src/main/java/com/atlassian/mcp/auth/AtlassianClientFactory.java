package com.atlassian.mcp.auth;

import com.atlassian.mcp.confluence.ConfluenceClient;
import com.atlassian.mcp.jira.JiraClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Atlassian 클라이언트 팩토리.
 * Python의 get_jira_fetcher(), get_confluence_fetcher()와 동일한 역할.
 * 
 * 매 요청마다 Bearer 토큰으로 새로운 Client 생성.
 */
@Service
public class AtlassianClientFactory {
    
    private static final Logger log = LoggerFactory.getLogger(AtlassianClientFactory.class);
    
    private final String jiraBaseUrl;
    private final String confluenceBaseUrl;
    private final ObjectMapper mapper;
    
    public AtlassianClientFactory(
            @Value("${atlassian.jira.baseUrl}") String jiraBaseUrl,
            @Value("${atlassian.confluence.baseUrl}") String confluenceBaseUrl,
            ObjectMapper mapper) {
        this.jiraBaseUrl = jiraBaseUrl;
        this.confluenceBaseUrl = confluenceBaseUrl;
        this.mapper = mapper;
    }
    
    /**
     * 현재 요청의 토큰으로 JiraClient 생성.
     * - security.mode=none: Authorization Bearer 토큰 사용
     * - security.mode=jwt: JIRA_TOKEN 헤더 사용
     */
    public JiraClient createJiraClient() {
        String token = getJiraToken();
        log.debug("Creating JiraClient");
        return new JiraClient(jiraBaseUrl, token, mapper);
    }
    
    /**
     * 현재 요청의 토큰으로 ConfluenceClient 생성.
     * - security.mode=none: Authorization Bearer 토큰 사용
     * - security.mode=jwt: CONFLUENCE_TOKEN 헤더 사용
     */
    public ConfluenceClient createConfluenceClient() {
        String token = getConfluenceToken();
        log.debug("Creating ConfluenceClient");
        return new ConfluenceClient(confluenceBaseUrl, token, mapper);
    }
    
    /**
     * 현재 요청의 Jira 토큰 가져오기.
     */
    private String getJiraToken() {
        HttpServletRequest request = getCurrentRequest();
        String token = (String) request.getAttribute("jira_token");
        
        if (token == null || token.isBlank()) {
            String authMode = (String) request.getAttribute("auth_mode");
            if ("jwt".equals(authMode)) {
                throw new IllegalStateException("No Jira token. JIRA_TOKEN header required in JWT mode.");
            } else {
                throw new IllegalStateException("No Jira token. Authorization: Bearer <token> required.");
            }
        }
        
        return token;
    }
    
    /**
     * 현재 요청의 Confluence 토큰 가져오기.
     */
    private String getConfluenceToken() {
        HttpServletRequest request = getCurrentRequest();
        String token = (String) request.getAttribute("confluence_token");
        
        if (token == null || token.isBlank()) {
            String authMode = (String) request.getAttribute("auth_mode");
            if ("jwt".equals(authMode)) {
                throw new IllegalStateException("No Confluence token. CONFLUENCE_TOKEN header required in JWT mode.");
            } else {
                throw new IllegalStateException("No Confluence token. Authorization: Bearer <token> required.");
            }
        }
        
        return token;
    }
    
    /**
     * 현재 HTTP 요청 가져오기.
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new IllegalStateException("Not in a web request context");
        }
        return attrs.getRequest();
    }
}
