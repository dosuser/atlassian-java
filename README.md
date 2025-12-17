# MCP Atlassian Server (Java)

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen?logo=spring)
![Maven](https://img.shields.io/badge/Maven-3.8+-blue?logo=apache-maven)
![License](https://img.shields.io/badge/License-MIT-yellow)
![MCP](https://img.shields.io/badge/MCP-1.0-purple)

**A high-performance Model Context Protocol (MCP) server for Atlassian products**

[Features](#-features) • [Quick Start](#-quick-start) • [Documentation](#-documentation) • [Contributing](#-contributing)

</div>

---

## 📋 Overview

A production-ready Java port of [mcp-atlassian](https://github.com/modelcontextprotocol/servers/tree/main/src/atlassian) providing MCP tools for seamless interaction with Jira and Confluence. Built with Spring Boot 3.3 and Java 21, featuring modern async/reactive patterns and enterprise-grade logging.

## ✨ Features

- 🚀 **Modern Stack**: Spring Boot 3.3 + Java 21 with Virtual Threads
- 🔄 **Reactive**: WebFlux-based async HTTP client for optimal performance
- 🔌 **MCP Protocol**: Latest HTTP POST protocol (no SSE, no streaming)
- 🛠️ **42 Tools**: Complete Jira (31) and Confluence (11) tool coverage
- 🔐 **Dual Auth**: Bearer token (simple) and JWT (enterprise) modes via Servlet Filters
- 📊 **Enterprise Logging**: Log4j2 with audit trail and external library support
- 🎯 **Request-Scoped**: Pythonic client factory pattern for multi-user support
- ⚡ **High Performance**: Non-blocking I/O with Project Reactor
- 📦 **Production Ready**: Docker support, comprehensive testing, CI/CD ready
- 🔒 **No Spring Security**: Lightweight servlet-based authentication for minimal overhead

## 🛠️ Supported Tools

<details>
<summary><b>Jira Tools (31/31)</b> ✅</summary>

### Read Operations (16)
- `jira_get_issue` - Fetch issue details
- `jira_search` - JQL-based search
- `jira_get_user_profile` - User profile lookup
- `jira_search_fields` - Field metadata search
- `jira_get_project_issues` - List project issues
- `jira_get_transitions` - Available issue transitions
- `jira_get_worklog` - Worklog entries
- `jira_get_agile_boards` - Agile board list
- `jira_get_board_issues` - Board backlog/sprint issues
- `jira_get_sprints_from_board` - Sprint list
- `jira_get_sprint_issues` - Sprint issues
- `jira_get_link_types` - Issue link types
- `jira_get_project_versions` - Project versions/releases
- `jira_get_all_projects` - All accessible projects
- `jira_batch_get_changelogs` - Bulk changelog retrieval
- `jira_download_attachments` - Attachment download

### Write Operations (15)
- `jira_create_issue` - Create new issue
- `jira_update_issue` - Update issue fields
- `jira_delete_issue` - Delete issue
- `jira_add_comment` - Add comment
- `jira_add_worklog` - Log work
- `jira_transition_issue` - Change issue status
- `jira_create_issue_link` - Link issues
- `jira_create_remote_issue_link` - Create remote link
- `jira_remove_issue_link` - Remove link
- `jira_link_to_epic` - Link to epic
- `jira_batch_create_issues` - Bulk issue creation
- `jira_create_sprint` - Create sprint
- `jira_update_sprint` - Update sprint
- `jira_create_version` - Create version
- `jira_batch_create_versions` - Bulk version creation

</details>

<details>
<summary><b>Confluence Tools (11/11)</b> ✅</summary>

- `confluence_get_page` - Fetch page content
- `confluence_search` - CQL search
- `confluence_get_page_children` - Child pages
- `confluence_get_comments` - Page comments
- `confluence_get_labels` - Page labels
- `confluence_add_label` - Add label to page
- `confluence_create_page` - Create new page
- `confluence_update_page` - Update page content
- `confluence_delete_page` - Delete page
- `confluence_add_comment` - Add comment
- `confluence_search_user` - User search

</details>

## 🚀 Quick Start

### Prerequisites

- **Java 21+** ([Download](https://adoptium.net/))
- **Maven 3.8+** ([Install](https://maven.apache.org/install.html))
- **Atlassian API Token** ([Generate](https://id.atlassian.com/manage-profile/security/api-tokens))

### Installation

```bash
# Clone the repository
git clone https://github.com/yourusername/mcp-atlassian-java.git
cd mcp-atlassian-java

# Build the project
mvn clean package
```

### Configuration

**Option 1: Environment Variables** (Recommended)

```bash
export SECURITY_MODE=none
export JIRA_BASE_URL=https://your-domain.atlassian.net
export CONFLUENCE_BASE_URL=https://your-domain.atlassian.net/wiki
```

**Option 2: `.env` File**

```bash
SECURITY_MODE=none
JIRA_BASE_URL=https://your-domain.atlassian.net
CONFLUENCE_BASE_URL=https://your-domain.atlassian.net/wiki
```

### Configure Environment

```bash
# Copy the example environment file
cp .env.example .env

# Edit .env with your actual values
# JIRA_BASE_URL=https://your-jira.atlassian.net
# CONFLUENCE_BASE_URL=https://your-confluence.atlassian.net
```

### Run the Server

```bash
mvn spring-boot:run
```

Server starts at `http://localhost:8080`

### Test the Connection

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Authorization: Bearer YOUR_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"1","type":"REQUEST","method":"jira_search","params":{"jql":"project=PROJ","limit":5}}'
```

## 💡 Usage Examples

### Basic Tool Invocation

```bash
# Get Jira issue
curl -X POST http://localhost:8080/mcp \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"1","type":"REQUEST","method":"jira_get_issue","params":{"issue_key":"PROJ-123"}}'

# Search with JQL
curl -X POST http://localhost:8080/mcp \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"2","type":"REQUEST","method":"jira_search","params":{"jql":"assignee=currentUser() AND status=Open","limit":20}}'

# Get Confluence page
curl -X POST http://localhost:8080/mcp \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"3","type":"REQUEST","method":"confluence_get_page","params":{"page_id":"123456"}}'

# Create Jira issue
curl -X POST http://localhost:8080/mcp \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"4","type":"REQUEST","method":"jira_create_issue","params":{"project":"PROJ","summary":"Bug fix","issue_type":"Task"}}'
```

### Response Format

**Success:**
```json
{
  "id": "1",
  "type": "RESPONSE",
  "result": {
    "success": true,
    "key": "PROJ-123",
    "fields": { ... }
  }
}
```

**Error:**
```json
{
  "id": "1",
  "type": "ERROR",
  "error": "Missing required parameter: issue_key"
}
```

## 🏗️ Architecture

```
com.atlassian.mcp/
├── auth/                    # Authentication filters
│   ├── BearerTokenFilter   # Simple Bearer auth (default)
│   ├── JwtAuthenticationFilter  # JWT validation
│   └── JwtService          # JWT utilities
├── core/                    # MCP protocol core
│   ├── ToolRegistry        # Dynamic tool registration
│   └── McpMessage          # Protocol message types
├── jira/                    # Jira integration
│   ├── JiraClient          # REST API client
│   └── JiraTools           # Tool implementations
├── confluence/              # Confluence integration
│   ├── ConfluenceClient    # REST API client
│   └── ConfluenceTools     # Tool implementations
├── factory/                 # Client factories
│   └── AtlassianClientFactory  # Request-scoped clients
└── server/                  # Spring Boot app
    ├── Application         # Main entry point
    ├── Config              # Bean configuration
    └── McpStreamController # HTTP endpoint
```

## 📚 Documentation

- **[Authentication Guide](docs/AUTHENTICATION.md)** - Bearer vs JWT modes
- **[Logging Guide](docs/LOGGING.md)** - Log4j2 configuration
- **[API Reference](docs/API.md)** - All 42 tools documented
- **[Contributing Guide](CONTRIBUTING.md)** - Development guidelines
- **[Security Policy](SECURITY.md)** - Security best practices

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Integration tests
mvn verify -P integration-tests
```

**Test Coverage:** 22 unit tests, 100% pass rate

## 🚢 Deployment

### Docker

```bash
# Build image
docker build -t mcp-atlassian-java .

# Run container
docker run -p 8080:8080 \
  -e SECURITY_MODE=jwt \
  -e JWT_SECRET=your-secret \
  mcp-atlassian-java
```

### Kubernetes

```bash
kubectl apply -f k8s/deployment.yaml
```

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Based on the original [mcp-atlassian](https://github.com/modelcontextprotocol/servers/tree/main/src/atlassian) Python implementation
- Built with ❤️ using Spring Boot and Project Reactor

## 📞 Support

- 📖 [Documentation](docs/)
- 🐛 [Issue Tracker](https://github.com/yourusername/mcp-atlassian-java/issues)
- 💬 [Discussions](https://github.com/yourusername/mcp-atlassian-java/discussions)

---

<div align="center">

**[⬆ Back to Top](#mcp-atlassian-server-java)**

</div>
