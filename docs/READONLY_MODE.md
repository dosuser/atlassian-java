# Readonly Mode

## Overview

The MCP server supports a readonly mode that prevents write operations when enabled. This is useful for:
- Read-only access to Atlassian resources
- Audit and reporting scenarios
- Safe exploration without modification risk
- Compliance with security policies

## How to Enable

Add the `X-Readonly: true` header to your MCP requests.

### Example with HTTP Client

```http
POST http://localhost:8080/mcp
Content-Type: application/json
X-Readonly: true

{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/list",
  "params": {}
}
```

### Example with curl

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "X-Readonly: true" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list",
    "params": {}
  }'
```

## Behavior

### When Readonly Mode is Enabled (`X-Readonly: true`)

1. **Tool List Filtering**: The `tools/list` endpoint returns only read-only tools
   - Confluence: `get_page`, `search`, `get_comments`, `get_labels`, `get_page_children`, `search_user`
   - Jira: `search`, `get_issue`, `get_project_issues`, `get_transitions`, `get_worklog`, etc.

2. **Write Operation Blocking**: Attempts to call write tools return an error
   ```json
   {
     "jsonrpc": "2.0",
     "id": 1,
     "error": {
       "code": -32000,
       "message": "Write operations not allowed in readonly mode"
     }
   }
   ```

3. **Blocked Tools**:
   - Confluence: `create_page`, `update_page`, `delete_page`, `add_label`, `add_comment`
   - Jira: `create_issue`, `update_issue`, `delete_issue`, `add_comment`, `add_worklog`, `transition_issue`, `link_to_epic`, `create_version`, etc.

### When Readonly Mode is Disabled (default)

All tools are available and can be executed normally.

## Implementation Details

### ToolMetadata

Each tool is registered with a `readOnly` flag:

```java
// Read-only tool (default)
registry.register(
    "confluence_get_page",
    "Get content of a specific page...",
    inputSchema,
    true, // readOnly flag
    handler
);

// Write tool
registry.register(
    "confluence_create_page",
    "Create a new page...",
    inputSchema,
    false, // write operation
    handler
);
```

### Controller Logic

The `McpStreamController` checks the `X-Readonly` header and filters/blocks accordingly:

```java
String readonlyHeader = httpRequest.getHeader("X-Readonly");
boolean isReadonly = "true".equalsIgnoreCase(readonlyHeader);

if (isReadonly) {
    // Filter write tools from list
    // Block write tool execution
}
```

## Configuration

No additional configuration is required. The readonly mode is controlled entirely by the request header.

## Security Considerations

- Readonly mode is enforced at the MCP protocol level
- Write operations are blocked before reaching the Atlassian APIs
- The mode is request-scoped (each request can specify its own mode)
- For session-based readonly enforcement, implement this at the client level

## Testing

Test readonly mode with the following scenarios:

1. **List tools in readonly mode**: Verify only read tools are returned
2. **Call read tool**: Should succeed
3. **Call write tool**: Should fail with error code -32000
4. **List tools without header**: Verify all tools are returned
5. **Call write tool without header**: Should succeed

Example test:
```java
@Test
void testReadonlyMode() {
    // Set readonly header
    request.addHeader("X-Readonly", "true");
    
    // Call write tool - should fail
    McpResponse response = controller.handleMcpRequest(
        new McpRequest("tools/call", Map.of(
            "name", "jira_create_issue",
            "arguments", Map.of(...)
        )),
        request
    );
    
    // Verify error
    assertEquals(-32000, response.getError().getCode());
    assertTrue(response.getError().getMessage()
        .contains("Write operations not allowed"));
}
```
