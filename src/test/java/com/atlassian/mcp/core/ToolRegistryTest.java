package com.atlassian.mcp.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolRegistryTest {

    @Test
    void testRegisterAndInvoke() {
        ToolRegistry registry = new ToolRegistry();
        
        // 간단한 echo 도구 등록
        registry.register("test_echo", params -> params);
        
        assertTrue(registry.has("test_echo"));
        assertFalse(registry.has("non_existent"));
        
        Object result = registry.invoke("test_echo", "hello");
        assertEquals("hello", result);
    }
    
    @Test
    void testInvokeUnknownTool() {
        ToolRegistry registry = new ToolRegistry();
        
        assertThrows(IllegalArgumentException.class, () -> {
            registry.invoke("unknown_tool", null);
        });
    }
}
