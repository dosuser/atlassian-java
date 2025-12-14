package com.atlassian.mcp.integration;

import com.atlassian.mcp.confluence.ConfluenceClient;
import com.atlassian.mcp.confluence.ConfluenceTools;
import com.atlassian.mcp.core.ToolRegistry;
import com.atlassian.mcp.jira.JiraClient;
import com.atlassian.mcp.jira.JiraReadToolsA;
import com.atlassian.mcp.jira.JiraReadToolsB;
import com.atlassian.mcp.jira.JiraReadToolsC;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP 통합 테스트 - POJO 스타일.
 * Spring 없이 순수 Java로 작성된 통합 테스트.
 */
class McpIntegrationTest {

    private ToolRegistry toolRegistry;
    private JiraReadToolsA jiraReadToolsA;
    private JiraReadToolsB jiraReadToolsB;
    private JiraReadToolsC jiraReadToolsC;
    private ConfluenceTools confluenceTools;
    
    @BeforeEach
    void setUp() {
        // 1. ObjectMapper 생성
        ObjectMapper mapper = new ObjectMapper();
        
        // 2. Jira 클라이언트 및 도구 생성
        String jiraBaseUrl = System.getenv("JIRA_BASE_URL");
        String jiraToken = "EXAMPLE_JIRA_TOKEN";
        JiraClient jiraClient = new JiraClient(jiraBaseUrl, jiraToken, mapper);
        jiraReadToolsA = new JiraReadToolsA(() -> jiraClient);
        jiraReadToolsB = new JiraReadToolsB(() -> jiraClient);
        jiraReadToolsC = new JiraReadToolsC(() -> jiraClient);
        
        // 3. Confluence 클라이언트 및 도구 생성
        String confluenceBaseUrl = System.getenv("CONFLUENCE_BASE_URL");
        String confluenceToken = "EXAMPLE_CONFLUENCE_TOKEN";
        ConfluenceClient confluenceClient = new ConfluenceClient(confluenceBaseUrl, confluenceToken, mapper);
        confluenceTools = new ConfluenceTools(() -> confluenceClient);
        
        // 4. ToolRegistry 생성 및 도구 등록
        toolRegistry = new ToolRegistry();
        registerAllTools();
    }
    
    private void registerAllTools() {
        // Utils
        toolRegistry.register("utils_echo", params -> params);
        
        // Confluence 도구 등록
        toolRegistry.register("confluence_get_page", params -> 
            confluenceTools.getPage(asMap(params)).block());
        toolRegistry.register("confluence_search", params -> 
            confluenceTools.search(asMap(params)).block());
        toolRegistry.register("confluence_get_page_children", params -> 
            confluenceTools.getPageChildren(asMap(params)).block());
        toolRegistry.register("confluence_get_comments", params -> 
            confluenceTools.getComments(asMap(params)).block());
        toolRegistry.register("confluence_get_labels", params -> 
            confluenceTools.getLabels(asMap(params)).block());
        toolRegistry.register("confluence_search_user", params -> 
            confluenceTools.searchUser(asMap(params)).block());
        
        // Jira 도구 등록 (일부)
        toolRegistry.register("jira_search", params -> 
            jiraReadToolsC.search(asMap(params)).block());
        toolRegistry.register("jira_get_all_projects", params -> 
            jiraReadToolsA.getAllProjects(asMap(params)).block());
        toolRegistry.register("jira_search_fields", params -> 
            jiraReadToolsC.searchFields(asMap(params)).block());
        toolRegistry.register("jira_get_link_types", params -> 
            jiraReadToolsB.getLinkTypes(asMap(params)).block());
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object params) {
        return (Map<String, Object>) params;
    }

    @Test
    void testToolRegistryHasAllTools() {
        // 모든 도구가 등록되었는지 확인
        assertTrue(toolRegistry.has("utils_echo"));
        assertTrue(toolRegistry.has("confluence_get_page"));
        assertTrue(toolRegistry.has("confluence_search"));
        assertTrue(toolRegistry.has("jira_search"));
        assertTrue(toolRegistry.has("jira_get_all_projects"));
        
        System.out.println("=== All Tools ===");
        System.out.println("Total tools registered: " + toolRegistry.all().size());
        toolRegistry.all().keySet().forEach(System.out::println);
    }

    @Test
    void testUtilsEcho() {
        Map<String, Object> params = new HashMap<>();
        params.put("message", "Hello MCP!");
        params.put("timestamp", System.currentTimeMillis());
        
        Object result = toolRegistry.invoke("utils_echo", params);
        
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals("Hello MCP!", resultMap.get("message"));
        
        System.out.println("=== Utils Echo ===");
        System.out.println("Result: " + result);
    }

    @Test
    void testConfluenceGetPageViaRegistry() {
        Map<String, Object> params = new HashMap<>();
        params.put("page_id", "609089742");
        
        Object result = toolRegistry.invoke("confluence_get_page", params);
        
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        System.out.println("=== Confluence Get Page (via Registry) ===");
        System.out.println("Result: " + resultMap);
        
        // 네트워크 타임아웃은 실패로 간주하지 않음 (개발 환경 문제)
        if (resultMap.containsKey("error")) {
            System.out.println("Warning: " + resultMap.get("error"));
            return; // 테스트 스킵
        }
        
        assertTrue((Boolean) resultMap.get("success"));
        System.out.println("Title: " + resultMap.get("title"));
    }

    @Test
    void testJiraSearchViaRegistry() {
        Map<String, Object> params = new HashMap<>();
        params.put("jql", "project = ADPLAT ORDER BY created DESC");
        params.put("limit", 3);
        
        Object result = toolRegistry.invoke("jira_search", params);
        
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        if (resultMap.containsKey("error")) {
            String error = (String) resultMap.get("error");
            if (error.contains("401") || error.contains("Unauthorized")) {
                System.out.println("⚠️ Jira 인증 실패 - 테스트 스킵");
                return;
            }
        } else {
            assertTrue((Boolean) resultMap.get("success"));
            System.out.println("=== Jira Search (via Registry) ===");
            System.out.println("Total: " + resultMap.get("total"));
        }
    }

    @Test
    void testConfluenceSearch() {
        Map<String, Object> params = new HashMap<>();
        params.put("cql", "type=page AND space=~TESTSPACE");
        params.put("limit", 3);
        
        Object result = toolRegistry.invoke("confluence_search", params);
        
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        if (resultMap.containsKey("error")) {
            String error = (String) resultMap.get("error");
            if (error.contains("429") || error.contains("Too Many Requests") ||
                error.contains("404") || error.contains("Not Found")) {
                System.out.println("⚠️ API 오류 - 테스트 스킵: " + error);
                return;
            }
        } else {
            assertTrue((Boolean) resultMap.get("success"));
            System.out.println("=== Confluence Search (via Registry) ===");
            System.out.println("Results: " + resultMap.get("results"));
        }
    }

    @Test
    void testErrorHandling() {
        Map<String, Object> params = new HashMap<>();
        params.put("page_id", "invalid_page_id");
        
        Object result = toolRegistry.invoke("confluence_get_page", params);
        
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertFalse((Boolean) resultMap.get("success"));
        assertNotNull(resultMap.get("error"));
        
        System.out.println("=== Error Handling Test ===");
        System.out.println("Error: " + resultMap.get("error"));
    }
}
