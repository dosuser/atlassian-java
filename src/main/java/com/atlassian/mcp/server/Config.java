package com.atlassian.mcp.server;

import com.atlassian.mcp.auth.AtlassianClientFactory;
import com.atlassian.mcp.core.ToolRegistry;
import com.atlassian.mcp.confluence.ConfluenceTools;
import com.atlassian.mcp.jira.JiraReadToolsA;
import com.atlassian.mcp.jira.JiraReadToolsB;
import com.atlassian.mcp.jira.JiraReadToolsC;
import com.atlassian.mcp.jira.JiraWriteTools;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * MCP 도구 설정.
 * Python dependencies.py와 동일한 역할: 요청별로 Client를 동적 생성.
 * 도구 등록은 JiraToolsConfig, ConfluenceToolsConfig에 위임.
 */
@Configuration
public class Config {
    
    private final AtlassianClientFactory clientFactory;
    
    public Config(AtlassianClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }
    
    @Bean
    public JiraReadToolsA jiraReadToolsA() {
        return new JiraReadToolsA(clientFactory::createJiraClient);
    }
    
    @Bean
    public JiraReadToolsB jiraReadToolsB() {
        return new JiraReadToolsB(clientFactory::createJiraClient);
    }
    
    @Bean
    public JiraReadToolsC jiraReadToolsC() {
        return new JiraReadToolsC(clientFactory::createJiraClient);
    }
    
    @Bean
    public JiraWriteTools jiraWriteTools() {
        return new JiraWriteTools(clientFactory::createJiraClient);
    }
    
    @Bean
    public ConfluenceTools confluenceTools() {
        return new ConfluenceTools(clientFactory::createConfluenceClient);
    }
    
    @Bean
    public ToolRegistry toolRegistry(
            JiraReadToolsA jiraReadToolsA,
            JiraReadToolsB jiraReadToolsB,
            JiraReadToolsC jiraReadToolsC,
            JiraWriteTools jiraWriteTools,
            ConfluenceTools confluenceTools) {
        ToolRegistry reg = new ToolRegistry();
        
        // 샘플 도구 (테스트용)
        reg.register(
            "utils_echo",
            "Echo test tool that returns the input parameters",
            Map.of("type", "object", "properties", Map.of()),
            params -> params
        );
        
        // Jira 도구 등록 (JiraToolsConfig에 위임)
        JiraToolsConfig.configure(reg, jiraReadToolsA, jiraReadToolsB, jiraReadToolsC, jiraWriteTools);
        
        // Confluence 도구 등록 (ConfluenceToolsConfig에 위임)
        ConfluenceToolsConfig.configure(reg, confluenceTools);
        
        return reg;
    }
}
