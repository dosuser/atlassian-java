package com.atlassian.mcp.core;

import lombok.Data;
import java.util.Map;

/**
 * Metadata for an Atlassian MCP tool.
 * Stores the tool's name, description, and JSON Schema for input parameters.
 */
@Data
public class ToolMetadata {
    private final String name;
    private final String description;
    private final Map<String, Object> inputSchema;
    private final boolean readOnly;
    
    public ToolMetadata(String name, String description, Map<String, Object> inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.readOnly = true; // default to read-only
    }
    
    public ToolMetadata(String name, String description, Map<String, Object> inputSchema, boolean readOnly) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.readOnly = readOnly;
    }
}
