package com.atlassian.mcp.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class McpMessageTest {

    @Test
    void testMessageCreation() {
        McpMessage msg = new McpMessage();
        msg.setId("test-123");
        msg.setType(McpMessage.Type.REQUEST);
        msg.setMethod("jira_get_issue");
        
        assertEquals("test-123", msg.getId());
        assertEquals(McpMessage.Type.REQUEST, msg.getType());
        assertEquals("jira_get_issue", msg.getMethod());
    }
    
    @Test
    void testResponseMessage() {
        McpMessage msg = new McpMessage();
        msg.setId("resp-456");
        msg.setType(McpMessage.Type.RESPONSE);
        msg.setResult("success");
        
        assertEquals("resp-456", msg.getId());
        assertEquals(McpMessage.Type.RESPONSE, msg.getType());
        assertEquals("success", msg.getResult());
        assertNull(msg.getError());
    }
    
    @Test
    void testErrorMessage() {
        McpMessage msg = new McpMessage();
        msg.setId("err-789");
        msg.setType(McpMessage.Type.ERROR);
        msg.setError("Something went wrong");
        
        assertEquals("err-789", msg.getId());
        assertEquals(McpMessage.Type.ERROR, msg.getType());
        assertEquals("Something went wrong", msg.getError());
        assertNull(msg.getResult());
    }
}
