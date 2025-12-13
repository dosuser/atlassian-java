package com.atlassian.mcp.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ToolRegistry {
    private final Map<String, Function<Object, Object>> tools = new ConcurrentHashMap<>();

    public void register(String name, Function<Object, Object> handler) {
        tools.put(name, handler);
    }

    public boolean has(String name) { return tools.containsKey(name); }

    public Object invoke(String name, Object params) {
        Function<Object, Object> fn = tools.get(name);
        if (fn == null) throw new IllegalArgumentException("Unknown tool: " + name);
        return fn.apply(params);
    }

    public Map<String, Function<Object, Object>> all() { return Map.copyOf(tools); }
}
