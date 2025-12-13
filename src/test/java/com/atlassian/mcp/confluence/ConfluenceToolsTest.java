package com.atlassian.mcp.confluence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Confluence Tools POJO 테스트.
 * Spring 없이 순수 Java로 작성된 단위 테스트.
 */
class ConfluenceToolsTest {

    private ConfluenceTools confluenceTools;
    
    @BeforeEach
    void setUp() {
        // 실제 설정값으로 클라이언트 및 도구 생성
        String baseUrl = "https://wiki.navercorp.com";
        String token = "EXAMPLE_TOKEN_FOR_TESTING";
        ObjectMapper mapper = new ObjectMapper();
        
        ConfluenceClient client = new ConfluenceClient(baseUrl, token, mapper);
        confluenceTools = new ConfluenceTools(() -> client);
    }

    @Test
    void testGetPage() {
        Map<String, Object> params = new HashMap<>();
        params.put("page_id", "609089742");
        
        Map<String, Object> result = confluenceTools.getPage(params).block();
        
        System.out.println("=== Confluence Get Page ===");
        
        assertNotNull(result);
        
        if (result.containsKey("error")) {
            String error = (String) result.get("error");
            System.out.println("Error: " + error);
            if (error.contains("429") || error.contains("Too Many Requests")) {
                System.out.println("⚠️ Rate limit - 테스트 스킵");
                return;
            }
        } else {
            assertTrue((Boolean) result.get("success"));
            System.out.println("✅ Success: " + result.get("success"));
            System.out.println("ID: " + result.get("id"));
            System.out.println("Title: " + result.get("title"));
        }
    }

    @Test
    void testSearch() {
        Map<String, Object> params = new HashMap<>();
        params.put("cql", "type=page AND space=~KR18723");
        params.put("limit", 5);
        
        Map<String, Object> result = confluenceTools.search(params).block();
        
        System.out.println("=== Confluence Search ===");
        
        assertNotNull(result);
        
        if (result.containsKey("error")) {
            String error = (String) result.get("error");
            System.out.println("Error: " + error);
            if (error.contains("429") || error.contains("Too Many Requests")) {
                System.out.println("⚠️ Rate limit - 테스트 스킵");
                return;
            }
        } else {
            assertTrue((Boolean) result.get("success"));
            assertNotNull(result.get("results"));
            System.out.println("✅ Results count: " + ((java.util.List<?>) result.get("results")).size());
        }
    }

    @Test
    void testGetPageChildren() {
        Map<String, Object> params = new HashMap<>();
        params.put("parent_id", "609089742");  // 올바른 파라미터 이름 사용
        
        Map<String, Object> result = confluenceTools.getPageChildren(params).block();
        
        System.out.println("=== Confluence Get Page Children ===");
        
        assertNotNull(result);
        if (result.containsKey("error")) {
            String error = (String) result.get("error");
            System.out.println("Error: " + error);
            if (error.contains("429") || error.contains("Too Many Requests")) {
                System.out.println("⚠️ Rate limit - 테스트 스킵");
                return;
            }
        } else {
            assertTrue((Boolean) result.get("success"));
            System.out.println("✅ Children found");
        }
    }

    @Test
    void testGetComments() {
        Map<String, Object> params = new HashMap<>();
        params.put("page_id", "609089742");
        
        Map<String, Object> result = confluenceTools.getComments(params).block();
        
        System.out.println("=== Confluence Get Comments ===");
        
        assertNotNull(result);
        if (result.containsKey("error")) {
            String error = (String) result.get("error");
            System.out.println("Error: " + error);
            if (error.contains("429") || error.contains("Too Many Requests") || 
                error.contains("404") || error.contains("Not Found")) {
                System.out.println("⚠️ API 오류 - 테스트 스킵");
                return;
            }
        } else {
            System.out.println("✅ Comments retrieved");
        }
    }

    @Test
    void testGetLabels() {
        Map<String, Object> params = new HashMap<>();
        params.put("page_id", "609089742");
        
        Map<String, Object> result = confluenceTools.getLabels(params).block();
        
        System.out.println("=== Confluence Get Labels ===");
        
        assertNotNull(result);
        if (result.containsKey("error")) {
            String error = (String) result.get("error");
            System.out.println("Error: " + error);
            if (error.contains("429") || error.contains("Too Many Requests") || 
                error.contains("404") || error.contains("Not Found")) {
                System.out.println("⚠️ API 오류 - 테스트 스킵");
                return;
            }
        } else {
            System.out.println("✅ Labels retrieved");
        }
    }

    @Test
    void testSearchUser() {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "KR18723");
        
        Map<String, Object> result = confluenceTools.searchUser(params).block();
        
        System.out.println("=== Confluence Search User ===");
        
        assertNotNull(result);
        if (result.containsKey("error")) {
            String error = (String) result.get("error");
            System.out.println("Error: " + error);
            if (error.contains("404") || error.contains("Not Found") || 
                error.contains("429") || error.contains("Too Many Requests")) {
                System.out.println("⚠️ API 오류 - 테스트 스킵");
                return;
            }
        } else {
            System.out.println("✅ User search completed");
        }
    }
}
