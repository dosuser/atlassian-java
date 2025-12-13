# Documentation Index

Welcome to the MCP Atlassian Server (Java) documentation!

## 📚 Main Documentation

### Getting Started
- **[README](../README.md)** - Project overview, quick start, and features
- **[Quick Start Guide](../README.md#-quick-start)** - Get up and running in 5 minutes

### Core Guides
- **[Authentication Guide](AUTHENTICATION.md)** - Bearer token vs JWT mode configuration
- **[Logging Guide](LOGGING.md)** - Log4j2 setup, custom configuration, audit logging
- **[API Reference](API.md)** - Complete reference for all 42 tools

### Development
- **[Contributing Guide](../CONTRIBUTING.md)** - How to contribute to the project
- **[Security Policy](../SECURITY.md)** - Security best practices and reporting

## 🔐 Authentication

Two modes supported:

| Mode | Use Case | Documentation |
|------|----------|---------------|
| **Bearer Token** | Development, single-user | [Guide](AUTHENTICATION.md#1-bearer-token-mode-default) |
| **JWT** | Production, multi-user | [Guide](AUTHENTICATION.md#2-jwt-mode-enterprise) |

**Quick Setup:**
```bash
# Bearer mode (simple)
export SECURITY_MODE=none

# JWT mode (enterprise)
export SECURITY_MODE=jwt
export JWT_SECRET=your-secret-key
```

## 📊 Logging

Enterprise-grade logging with Log4j2:

- **Default Configuration**: Works out of the box
- **Custom Configuration**: via `LOGGING_CONFIG` environment variable
- **External Libraries**: Place JARs in `lib/` folder
- **JWT Audit Logging**: Automatic in JWT mode

**Log Files:**
- `logs/atlassian-mcp.log` - All logs
- `logs/error.log` - Errors only
- `logs/jwt-audit.log` - JWT audit trail

[Full Logging Guide →](LOGGING.md)

## 🛠️ API Tools

### Jira Tools (31)

<details>
<summary>Read Operations (16 tools)</summary>

- `jira_get_issue`
- `jira_search`
- `jira_get_user_profile`
- `jira_search_fields`
- `jira_get_project_issues`
- `jira_get_transitions`
- `jira_get_worklog`
- `jira_get_agile_boards`
- `jira_get_board_issues`
- `jira_get_sprints_from_board`
- `jira_get_sprint_issues`
- `jira_get_link_types`
- `jira_get_project_versions`
- `jira_get_all_projects`
- `jira_batch_get_changelogs`
- `jira_download_attachments`

</details>

<details>
<summary>Write Operations (15 tools)</summary>

- `jira_create_issue`
- `jira_update_issue`
- `jira_delete_issue`
- `jira_add_comment`
- `jira_add_worklog`
- `jira_transition_issue`
- `jira_create_issue_link`
- `jira_create_remote_issue_link`
- `jira_remove_issue_link`
- `jira_link_to_epic`
- `jira_batch_create_issues`
- `jira_create_sprint`
- `jira_update_sprint`
- `jira_create_version`
- `jira_batch_create_versions`

</details>

### Confluence Tools (11)

- `confluence_search`
- `confluence_get_page`
- `confluence_get_page_children`
- `confluence_get_comments`
- `confluence_get_labels`
- `confluence_add_label`
- `confluence_create_page`
- `confluence_update_page`
- `confluence_delete_page`
- `confluence_add_comment`
- `confluence_search_user`

[Full API Reference →](API.md)

## 🚀 Deployment

### Docker

```bash
docker build -t mcp-atlassian-java .
docker run -p 8080:8080 \
  -e SECURITY_MODE=jwt \
  -e JWT_SECRET=your-secret \
  mcp-atlassian-java
```

### Kubernetes

```bash
kubectl apply -f k8s/deployment.yaml
```

### Production Checklist

Before deploying:

- [ ] `SECURITY_MODE=jwt` enabled
- [ ] Strong JWT secret configured
- [ ] HTTPS enabled
- [ ] Logging configured
- [ ] Monitoring setup
- [ ] Backups configured

[Security Policy →](../SECURITY.md)

## 🧪 Testing

```bash
# Run all tests
mvn test

# With coverage
mvn test jacoco:report

# Integration tests
mvn verify -P integration-tests
```

**Coverage:** 22 unit tests, 100% pass rate

## 🏗️ Architecture

```
com.atlassian.mcp/
├── auth/          # Authentication (Bearer/JWT)
├── core/          # MCP protocol
├── jira/          # Jira integration
├── confluence/    # Confluence integration
├── factory/       # Client factories
└── server/        # Spring Boot app
```

**Key Features:**
- Request-scoped clients (multi-user support)
- Conditional bean registration (auth modes)
- Reactive HTTP client (WebFlux)
- Async/non-blocking I/O

## 🔧 Configuration

### Environment Variables

```bash
# Authentication
SECURITY_MODE=none|jwt
JWT_SECRET=your-secret-key

# Services
JIRA_BASE_URL=https://your-domain.atlassian.net
CONFLUENCE_BASE_URL=https://your-domain.atlassian.net/wiki

# Logging
LOGGING_CONFIG=/path/to/log4j2.xml
LOGGING_LEVEL_COM_ATLASSIAN_MCP=INFO
```

### application.yml

```yaml
app:
  security:
    mode: ${SECURITY_MODE:none}
  jwt:
    secret: ${JWT_SECRET:}
```

## 🤝 Contributing

We welcome contributions!

1. **Read** [Contributing Guide](../CONTRIBUTING.md)
2. **Fork** the repository
3. **Create** feature branch
4. **Test** your changes
5. **Submit** pull request

### Development Setup

```bash
git clone https://github.com/your-fork/mcp-atlassian-java.git
cd mcp-atlassian-java
mvn clean install
mvn spring-boot:run
```

[Full Contributing Guide →](../CONTRIBUTING.md)

## 🐛 Troubleshooting

### Common Issues

**"JWT secret is required"**
- Solution: Set `JWT_SECRET` environment variable

**"No Jira token"**
- Bearer mode: Add `Authorization: Bearer <token>` header
- JWT mode: Add `JIRA_TOKEN: <token>` header

**401 Unauthorized**
- Verify API token is valid
- Check JWT signature/expiration
- Ensure correct headers

[More Troubleshooting →](AUTHENTICATION.md#troubleshooting)

## 📖 Additional Resources

### Internal Documentation
- [Migration Plan](../MIGRATION_PLAN.md) - Python to Java migration strategy
- [Project Structure](../PROJECT_STRUCTURE.md) - Codebase organization
- [Work Log](../WORKLOG.md) - Development progress
- [Testing Guide](../TESTING-POJO.md) - Unit testing approach
- [Spec Validation](../SPEC_VALIDATION.md) - Python compatibility report

### External Links
- [MCP Protocol Specification](https://modelcontextprotocol.io/)
- [Atlassian REST API](https://developer.atlassian.com/cloud/jira/platform/rest/v3/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Project Reactor](https://projectreactor.io/)

## 📞 Support

- **Documentation**: You're reading it!
- **Issues**: [GitHub Issues](https://github.com/yourusername/mcp-atlassian-java/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/mcp-atlassian-java/discussions)
- **Security**: See [Security Policy](../SECURITY.md)

## 📄 License

This project is licensed under the MIT License - see [LICENSE](../LICENSE) for details.

---

**Happy Coding!** 🚀
