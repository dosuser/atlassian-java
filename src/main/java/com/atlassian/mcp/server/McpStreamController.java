package com.atlassian.mcp.server;

import com.atlassian.mcp.auth.JwtAuditLogger;
import com.atlassian.mcp.core.McpRequest;
import com.atlassian.mcp.core.McpResponse;
import com.atlassian.mcp.core.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Protocol Controller
 * Handles JSON-RPC 2.0 requests according to MCP specification
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*")
public class McpStreamController {
    
    private final ToolRegistry registry;
    private final ObjectMapper mapper;
    private final JwtAuditLogger auditLogger;
    private final String securityMode;

    public McpStreamController(
            ToolRegistry registry, 
            ObjectMapper mapper,
            @Autowired(required = false) JwtAuditLogger auditLogger,
            @Value("${app.security.mode:none}") String securityMode) {
        this.registry = registry;
        this.mapper = mapper;
        this.auditLogger = auditLogger;
        this.securityMode = securityMode;
    }

    /**
     * MCP JSON-RPC 2.0 endpoint
     * POST /
     */
    @PostMapping(value = "", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public McpResponse handleMcpRequest(@RequestBody McpRequest request, HttpServletRequest httpRequest) {
        log.info("Received MCP request - method: {}, id: {}", request.getMethod(), request.getId());

        try {
            // Handle notification requests (no id, no response needed)
            if (request.getId() == null) {
                log.debug("Received notification: {}", request.getMethod());
                // For notifications, we still return a response but it will be ignored
                return handleNotification(request.getMethod());
            }
            
            return switch (request.getMethod()) {
                case "initialize" -> handleInitialize(request.getId(), request.getParams());
                case "initialized" -> handleInitialized(request.getId());
                case "ping" -> handlePing(request.getId());
                case "tools/list" -> handleToolsList(request.getId(), httpRequest);
                case "tools/call" -> handleToolsCall(request.getId(), request.getParams(), httpRequest);
                default -> {
                    // Try to invoke as a registered tool
                    try {
                        Object result = registry.invoke(request.getMethod(), request.getParams());
                        yield McpResponse.success(request.getId(), result);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid parameters: method={}, error={}", request.getMethod(), e.getMessage());
                        yield McpResponse.error(request.getId(), -32602, 
                            "Invalid params: " + e.getMessage());
                    } catch (Exception e) {
                        log.error("Tool invocation failed: method={}, error={}", request.getMethod(), e.getMessage());
                        yield McpResponse.error(request.getId(), -32603, 
                            "Internal error: " + e.getMessage());
                    }
                }
            };
        } catch (Exception e) {
            log.error("Error handling MCP request", e);
            return McpResponse.error(request.getId(), -32603, 
                "Internal error: " + e.getMessage());
        }
    }

    /**
     * Handle notification (no response expected)
     */
    private McpResponse handleNotification(String method) {
        log.debug("Processing notification: {}", method);
        // Return empty success response (will be ignored by client)
        return McpResponse.success(null, Map.of());
    }

    /**
     * Handle initialized notification
     */
    private McpResponse handleInitialized(Object id) {
        log.debug("Client initialized successfully");
        return McpResponse.success(id, Map.of());
    }

    /**
     * Handle ping request
     */
    private McpResponse handlePing(Object id) {
        log.debug("Client ping request");
        return McpResponse.success(id, Map.of());
    }

    /**
     * Handle initialize request
     */
    private McpResponse handleInitialize(Object id, Map<String, Object> params) {
        log.info("MCP initialize request received");
        
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("serverInfo", Map.of(
            "name", "mcp-atlassian-java",
            "version", "0.1.0"
        ));
        
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("tools", Map.of("listChanged", true));
        result.put("capabilities", capabilities);
        
        return McpResponse.success(id, result);
    }

    /**
     * GET /tools - List all available tools (JSON format)
     * Simple HTTP endpoint for viewing tool documentation
     */
    @GetMapping(value = "/tools", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getTools() {
        log.debug("HTTP GET /tools request");
        
        var toolMetadataList = registry.getAllMetadata();
        var tools = toolMetadataList.stream()
            .map(meta -> Map.of(
                "name", meta.getName(),
                "description", meta.getDescription(),
                "inputSchema", meta.getInputSchema()
            ))
            .toList();

        return Map.of(
            "serverInfo", Map.of(
                "name", "mcp-atlassian-java",
                "version", "0.1.0",
                "protocol", "MCP 2024-11-05"
            ),
            "totalTools", tools.size(),
            "tools", tools
        );
    }

    /**
     * Handle tools/list request
     */
    private McpResponse handleToolsList(Object id, HttpServletRequest httpRequest) {
        log.debug("Handling tools/list request");
        
        // Check for readonly header
        String readonlyHeader = httpRequest.getHeader("X-Readonly");
        boolean isReadonly = "true".equalsIgnoreCase(readonlyHeader);
        
        if (isReadonly) {
            log.info("Readonly mode enabled - filtering write tools from list");
        }
        
        var toolMetadataList = registry.getAllMetadata();
        var tools = toolMetadataList.stream()
            .filter(meta -> !isReadonly || meta.isReadOnly()) // Filter write tools if readonly
            .map(meta -> Map.of(
                "name", meta.getName(),
                "description", meta.getDescription(),
                "inputSchema", meta.getInputSchema()
            ))
            .toList();

        Map<String, Object> result = Map.of("tools", tools);
        return McpResponse.success(id, result);
    }

    /**
     * Handle tools/call request
     */
    private McpResponse handleToolsCall(Object id, Map<String, Object> params, HttpServletRequest httpRequest) {
        log.debug("Handling tools/call request with params: {}", params);

        if (params == null) {
            return McpResponse.error(id, -32602, "Invalid params: params is required");
        }

        String toolName = (String) params.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");

        // Check for readonly header
        String readonlyHeader = httpRequest.getHeader("X-Readonly");
        boolean isReadonly = "true".equalsIgnoreCase(readonlyHeader);
        
        // Check if tool is write operation when readonly mode is enabled
        if (isReadonly) {
            var metadata = registry.getAllMetadata().stream()
                .filter(m -> m.getName().equals(toolName))
                .findFirst();
            
            if (metadata.isPresent() && !metadata.get().isReadOnly()) {
                log.warn("Readonly mode: blocking write tool execution: {}", toolName);
                return McpResponse.error(id, -32000, "Write operations not allowed in readonly mode");
            }
        }

        // JWT audit logging
        if ("jwt".equals(securityMode) && auditLogger != null) {
            String userId = (String) httpRequest.getAttribute("user_id");
            if (userId != null) {
                auditLogger.logToolInvocation(userId, toolName, arguments);
            }
        }

        try {
            Object toolResult = registry.invoke(toolName, arguments);
            
            // Build MCP tool response with content array format
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> content = new ArrayList<>();
            
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", mapper.writeValueAsString(toolResult));
            content.add(textContent);
            
            result.put("content", content);
            result.put("isError", false);
            
            return McpResponse.success(id, result);
        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for tool {}: {}", toolName, e.getMessage());
            return McpResponse.error(id, -32602, "Invalid params: " + e.getMessage());
        } catch (Exception e) {
            log.error("Tool execution failed: {}", e.getMessage(), e);
            return McpResponse.error(id, -32603, "Tool execution failed: " + e.getMessage());
        }
    }
}
