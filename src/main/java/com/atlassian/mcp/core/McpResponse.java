package com.atlassian.mcp.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP JSON-RPC 2.0 Response
 * Only includes non-null fields in JSON serialization
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpResponse {
    private String jsonrpc = "2.0";
    private Object id;
    private Object result;
    private Map<String, Object> error;

    public static McpResponse success(Object id, Object result) {
        McpResponse response = new McpResponse();
        response.setJsonrpc("2.0");
        response.setId(id);
        response.setResult(result);
        return response;
    }

    public static McpResponse error(Object id, int code, String message) {
        McpResponse response = new McpResponse();
        response.setJsonrpc("2.0");
        response.setId(id);
        response.setError(Map.of("code", code, "message", message));
        return response;
    }
}
