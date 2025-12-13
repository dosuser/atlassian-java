# MCP Atlassian → Java Migration Plan

Audience: maintainers and contributors porting Python MCP Atlassian to Java (Spring Boot, JDK 21).

## Goals
- Port core MCP server and tools to Java with Spring Boot 3.x on JDK 21.
- Preserve tool parity: `{service}_{action}` naming; Jira/Confluence coverage.
- Implement MCP HTTP stream transport compatible with existing clients.
- Maintain testability, clear module boundaries, and observability.

## Scope & Strategy
1. Architecture: Spring Boot application with modules:
   - `mcp-core`: MCP protocol, stream transport, tool registry
   - `atlassian-jira`: Jira client, models, tool endpoints
   - `atlassian-confluence`: Confluence client, models, tool endpoints
   - `server`: Boot starter, wiring, configuration
2. Auth: API token, PAT, OAuth 2.0 (Authorization Code) via Spring Security OAuth Client.
3. Models: Pydantic → Java records/POJOs with validation (Jakarta Validation).
4. Tools: map Python tools to Spring `@Service` methods exposed via MCP tool registry.
5. Config: externalized via `application.yml`, profiles, environment variables.
6. Testing: JUnit 5 + Mockito; integration tests with Testcontainers when needed.

## Tool Inventory & Mapping
Follow `{service}_{action}` naming; examples below. Full inventory to be populated in WORKLOG.

- Jira
  - `jira_create_issue` → `JiraTools.createIssue()`
  - `jira_get_issue` → `JiraTools.getIssue()`
  - `jira_search_issues` → `JiraTools.searchIssues()`
  - `jira_transition_issue` → `JiraTools.transitionIssue()`
- Confluence
  - `confluence_get_page` → `ConfluenceTools.getPage()`
  - `confluence_search` → `ConfluenceTools.search()`
  - `confluence_update_page` → `ConfluenceTools.updatePage()`

## MCP HTTP Stream Design (High-level)
- Transport: Full-duplex HTTP stream using `text/event-stream` (SSE) or `application/x-ndjson` with backpressure.
- Protocol: JSON messages with `id`, `type`, `method`, `params`, `result`, `error` fields.
- Endpoints:
  - `POST /mcp/stream` → upgrades to streaming; bidirectional via request/response body streaming.
  - Optional: `GET /mcp/health` and `GET /mcp/tools` for discovery.
- Serialization: Jackson; message framing via newline-delimited JSON.
- Concurrency: Virtual threads (Project Loom) via `Executors.newVirtualThreadPerTaskExecutor()`.

## Modules & Packages
- `com.atlassian.mcp.core` → protocol, registry, message types, streaming
- `com.atlassian.mcp.jira` → Jira client (REST), models, tools
- `com.atlassian.mcp.confluence` → Confluence client (REST), models, tools
- `com.atlassian.mcp.server` → Spring Boot app, controllers, config

## Configuration & Secrets
- `application.yml` keys:
  - `mcp.transport.streamFormat` (ndjson|sse)
  - `atlassian.jira.baseUrl`, `token`, `oauth.*`
  - `atlassian.confluence.baseUrl`, `token`, `oauth.*`
- Secrets via environment or Spring Cloud Config; no hardcoding.

## Testing Plan
- Unit tests for protocol parsing, tool mapping, and clients.
- Integration tests for streaming endpoint and auth flows.
- Optional Testcontainers for mock Atlassian endpoints.

## Migration Steps
1. Scaffold Spring Boot project (Java 21, Maven or Gradle) and baseline modules.
2. Implement MCP core types and stream transport.
3. Port Jira tools: clients, models, service methods; add tests.
4. Port Confluence tools similarly.
5. Add OAuth flow and configuration mapping.
6. Validate parity against Python tests conceptually; add Java tests.
7. Document run/test steps and CI configuration.

## Deliverables
- Working Spring Boot app providing MCP HTTP stream endpoint.
- Tool parity docs and checklist.
- Runbook and usage examples.
- Tests passing and clear configuration guidance.
