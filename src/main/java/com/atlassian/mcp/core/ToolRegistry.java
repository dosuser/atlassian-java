package com.atlassian.mcp.core;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ToolRegistry {
    private final Map<String, Function<Object, Object>> tools = new ConcurrentHashMap<>();
    private final Map<String, ToolMetadata> metadata = new ConcurrentHashMap<>();

    /**
     * Register a tool with its metadata and handler function.
     *
     * @param name Tool name
     * @param description Tool description
     * @param inputSchema JSON Schema for input parameters
     * @param handler Function to handle tool invocations
     */
    public void register(String name, String description, Map<String, Object> inputSchema, Function<Object, Object> handler) {
        tools.put(name, handler);
        metadata.put(name, new ToolMetadata(name, description, inputSchema, true)); // default to read-only
    }

    /**
     * Register a tool with its metadata, handler function, and readonly flag.
     *
     * @param name Tool name
     * @param description Tool description
     * @param inputSchema JSON Schema for input parameters
     * @param readOnly Whether this tool is read-only (true) or write operation (false)
     * @param handler Function to handle tool invocations
     */
    public void register(String name, String description, Map<String, Object> inputSchema, boolean readOnly, Function<Object, Object> handler) {
        tools.put(name, handler);
        metadata.put(name, new ToolMetadata(name, description, inputSchema, readOnly));
    }

    /**
     * Legacy method for backward compatibility - registers tool without metadata.
     */
    public void register(String name, Function<Object, Object> handler) {
        tools.put(name, handler);
        // Create minimal metadata with placeholder description
        metadata.put(name, new ToolMetadata(name, "Atlassian tool: " + name, Map.of()));
    }

    public boolean has(String name) { 
        return tools.containsKey(name); 
    }

    public Object invoke(String name, Object params) {
        Function<Object, Object> fn = tools.get(name);
        if (fn == null) throw new IllegalArgumentException("Unknown tool: " + name);
        return fn.apply(params);
    }

    /**
     * Get metadata for a specific tool.
     *
     * @param name Tool name
     * @return ToolMetadata or null if not found
     */
    public ToolMetadata getMetadata(String name) {
        return metadata.get(name);
    }

    /**
     * Get metadata for all registered tools.
     *
     * @return Collection of all ToolMetadata objects
     */
    public Collection<ToolMetadata> getAllMetadata() {
        return metadata.values();
    }

    /**
     * Get all tool handler functions (legacy method).
     */
    public Map<String, Function<Object, Object>> all() { 
        return Map.copyOf(tools); 
    }
}
