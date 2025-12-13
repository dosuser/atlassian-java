package com.atlassian.mcp.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP JSON-RPC 2.0 Request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpRequest {
    private String jsonrpc = "2.0";
    private Object id;
    private String method;
    private Map<String, Object> params;
}
