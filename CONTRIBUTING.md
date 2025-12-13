# Contributing to MCP Atlassian Server (Java)

Thank you for your interest in contributing! This document provides guidelines and instructions for contributing to this project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Pull Request Process](#pull-request-process)
- [Issue Guidelines](#issue-guidelines)

## Code of Conduct

This project adheres to a code of conduct that all contributors are expected to follow. Please be respectful and constructive in all interactions.

## Getting Started

### Prerequisites

- Java 21 or later
- Maven 3.8 or later
- Git
- IDE with Java support (IntelliJ IDEA, Eclipse, VS Code)

### Setup Development Environment

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/mcp-atlassian-java.git
cd mcp-atlassian-java

# Add upstream remote
git remote add upstream https://github.com/ORIGINAL_OWNER/mcp-atlassian-java.git

# Build the project
mvn clean install

# Run tests
mvn test
```

### Running the Server Locally

```bash
# Set environment variables
export SECURITY_MODE=none
export JIRA_BASE_URL=https://your-domain.atlassian.net
export CONFLUENCE_BASE_URL=https://your-domain.atlassian.net/wiki

# Run the application
mvn spring-boot:run
```

## Development Workflow

### 1. Create a Branch

```bash
# Update your main branch
git checkout main
git pull upstream main

# Create a feature branch
git checkout -b feature/your-feature-name

# Or for bug fixes
git checkout -b fix/issue-description
```

### 2. Make Changes

- Write clean, readable code
- Follow the existing code style
- Add tests for new functionality
- Update documentation as needed

### 3. Commit Changes

```bash
# Stage your changes
git add .

# Commit with a descriptive message
git commit -m "feat: add new tool for Jira sprints"
```

**Commit Message Format:**

```
<type>: <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Example:**

```
feat: add jira_get_sprint_velocity tool

Implement new tool to retrieve sprint velocity metrics including
completed story points and velocity trend.

Closes #123
```

### 4. Push Changes

```bash
git push origin feature/your-feature-name
```

### 5. Open Pull Request

- Go to GitHub and open a Pull Request
- Fill out the PR template completely
- Link related issues
- Request review from maintainers

## Coding Standards

### Java Code Style

- **Formatting**: Follow standard Java conventions
- **Line Length**: Maximum 120 characters
- **Indentation**: 4 spaces (no tabs)
- **Naming**:
  - Classes: `PascalCase`
  - Methods/Variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase`

### Best Practices

1. **SOLID Principles**: Follow SOLID design principles
2. **DRY**: Don't Repeat Yourself - extract common code
3. **Clean Code**: Write self-documenting code with meaningful names
4. **Error Handling**: Proper exception handling and logging
5. **Javadoc**: Document public APIs with Javadoc comments

### Example Code Style

```java
package com.atlassian.mcp.jira;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Provides Jira-related MCP tools.
 * 
 * @author Your Name
 * @since 1.0.0
 */
@Component
public class JiraTools {
    
    private static final int DEFAULT_LIMIT = 50;
    
    private final JiraClient client;
    
    /**
     * Constructs a new JiraTools instance.
     *
     * @param client the Jira REST API client
     */
    public JiraTools(JiraClient client) {
        this.client = client;
    }
    
    /**
     * Searches Jira issues using JQL.
     *
     * @param params parameters including jql query and limit
     * @return Mono emitting search results
     */
    public Mono<Map<String, Object>> search(Map<String, Object> params) {
        String jql = (String) params.get("jql");
        int limit = (int) params.getOrDefault("limit", DEFAULT_LIMIT);
        
        return client.searchIssues(jql, limit)
            .map(this::toResponse)
            .onErrorResume(this::handleError);
    }
    
    private Map<String, Object> toResponse(JsonNode result) {
        // Implementation
    }
    
    private Mono<Map<String, Object>> handleError(Throwable error) {
        // Error handling
    }
}
```

## Testing

### Unit Tests

```java
@Test
void testSearchWithValidJql() {
    // Arrange
    Map<String, Object> params = new HashMap<>();
    params.put("jql", "project = TEST");
    params.put("limit", 10);
    
    // Act
    Map<String, Object> result = jiraTools.search(params).block();
    
    // Assert
    assertNotNull(result);
    assertTrue((Boolean) result.get("success"));
    assertNotNull(result.get("issues"));
}

@Test
void testSearchWithInvalidJql() {
    Map<String, Object> params = new HashMap<>();
    params.put("jql", "invalid jql syntax");
    
    Map<String, Object> result = jiraTools.search(params).block();
    
    assertNotNull(result);
    assertFalse((Boolean) result.get("success"));
    assertNotNull(result.get("error"));
}
```

### Integration Tests

```java
@SpringBootTest
@TestPropertySource(properties = {
    "atlassian.jira.baseUrl=https://test.atlassian.net",
    "atlassian.jira.token=test-token"
})
class JiraIntegrationTest {
    
    @Autowired
    private JiraTools jiraTools;
    
    @Test
    void testRealApiCall() {
        // Integration test with actual API
    }
}
```

### Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=JiraToolsTest

# With coverage
mvn test jacoco:report

# Skip tests during build
mvn package -DskipTests
```

## Pull Request Process

### Before Submitting

- [ ] All tests pass locally
- [ ] Code follows style guidelines
- [ ] New tests added for new features
- [ ] Documentation updated
- [ ] Commit messages follow conventions
- [ ] Branch is up to date with main

### PR Checklist

1. **Title**: Clear, descriptive title
2. **Description**: 
   - What changes were made?
   - Why were they made?
   - How were they implemented?
3. **Linked Issues**: Reference related issues
4. **Tests**: Describe test coverage
5. **Documentation**: List documentation updates
6. **Breaking Changes**: Note any breaking changes

### Review Process

1. Automated checks must pass (CI/CD)
2. At least one maintainer review required
3. Address all review comments
4. Keep PR scope focused and manageable
5. Rebase if needed to keep history clean

### After Approval

- Maintainer will merge using "Squash and Merge"
- Delete your feature branch after merge
- Update your local repository:

```bash
git checkout main
git pull upstream main
git push origin main
```

## Issue Guidelines

### Reporting Bugs

**Template:**

```markdown
**Describe the bug**
A clear description of what the bug is.

**To Reproduce**
Steps to reproduce:
1. Configure server with '...'
2. Call tool '...'
3. See error

**Expected behavior**
What you expected to happen.

**Actual behavior**
What actually happened.

**Environment**
- Java version:
- Spring Boot version:
- OS:
- Additional context:

**Logs**
```
Paste relevant logs here
```
```

### Feature Requests

**Template:**

```markdown
**Is your feature request related to a problem?**
Clear description of the problem.

**Describe the solution you'd like**
What you want to happen.

**Describe alternatives considered**
Other solutions you've considered.

**Additional context**
Any other context or screenshots.
```

### Questions

For questions, please use [GitHub Discussions](https://github.com/OWNER/REPO/discussions) instead of issues.

## Adding New Tools

### Step-by-Step Guide

1. **Define the tool** in `JiraTools.java` or `ConfluenceTools.java`:

```java
public Mono<Map<String, Object>> myNewTool(Map<String, Object> params) {
    // Validate parameters
    String required = (String) params.get("required_param");
    if (required == null) {
        return Mono.just(Map.of(
            "success", false,
            "error", "required_param is required"
        ));
    }
    
    // Call API
    return client.callApi(required)
        .map(result -> Map.of(
            "success", true,
            "data", result
        ))
        .onErrorResume(e -> Mono.just(Map.of(
            "success", false,
            "error", e.getMessage()
        )));
}
```

2. **Register the tool** in `Config.java`:

```java
@Bean
public ToolRegistry toolRegistry(JiraTools jiraTools) {
    ToolRegistry registry = new ToolRegistry();
    
    registry.register("my_new_tool", params -> {
        @SuppressWarnings("unchecked")
        Map<String, Object> paramsMap = (Map<String, Object>) params;
        return jiraTools.myNewTool(paramsMap).toFuture().join();
    });
    
    return registry;
}
```

3. **Add tests**:

```java
@Test
void testMyNewTool() {
    Map<String, Object> params = new HashMap<>();
    params.put("required_param", "value");
    
    Map<String, Object> result = jiraTools.myNewTool(params).block();
    
    assertNotNull(result);
    assertTrue((Boolean) result.get("success"));
}
```

4. **Update documentation** in README.md

## Documentation

### Required Documentation

- **Code Comments**: Complex logic should be commented
- **Javadoc**: All public APIs must have Javadoc
- **README**: Update if functionality changes
- **API Docs**: Document new tools in API.md
- **Changelog**: Add entry to CHANGELOG.md

### Documentation Style

```java
/**
 * Searches Jira issues using JQL (Jira Query Language).
 * 
 * <p>This method performs a JQL search and returns matching issues with
 * their fields. The result includes issue keys, summaries, and other
 * requested fields.</p>
 *
 * @param params the search parameters containing:
 *               - jql: JQL query string (required)
 *               - limit: maximum number of results (optional, default: 50)
 *               - fields: comma-separated field list (optional)
 * @return a Mono emitting a map with search results:
 *         - success: boolean indicating success
 *         - issues: array of matching issues
 *         - total: total number of matches
 * @throws IllegalArgumentException if jql parameter is missing
 */
public Mono<Map<String, Object>> search(Map<String, Object> params) {
    // Implementation
}
```

## Getting Help

- **Documentation**: Check the [docs](docs/) folder
- **Discussions**: Use [GitHub Discussions](https://github.com/OWNER/REPO/discussions)
- **Issues**: Search existing [issues](https://github.com/OWNER/REPO/issues)
- **Contact**: Reach out to maintainers

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (MIT License).

---

Thank you for contributing to MCP Atlassian Server! \ud83c\udf89
