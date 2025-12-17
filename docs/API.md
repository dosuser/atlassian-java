# API Reference

Complete reference for all 42 MCP tools provided by this server.

## Table of Contents

- [Jira Tools](#jira-tools)
  - [Read Operations](#jira-read-operations)
  - [Write Operations](#jira-write-operations)
- [Confluence Tools](#confluence-tools)
- [Utility Tools](#utility-tools)

---

## Jira Tools

### Jira Read Operations

#### `jira_get_issue`

Retrieve details of a specific Jira issue.

**Parameters:**
- `issue_key` (string, required): Issue key (e.g., "PROJ-123")
- `fields` (string, optional): Comma-separated fields to retrieve

**Example:**
```json
{
  "id": "1",
  "type": "REQUEST",
  "method": "jira_get_issue",
  "params": {
    "issue_key": "PROJ-123",
    "fields": "summary,status,assignee"
  }
}
```

**Response:**
```json
{
  "id": "1",
  "type": "RESPONSE",
  "result": {
    "success": true,
    "key": "PROJ-123",
    "fields": {
      "summary": "Fix login bug",
      "status": {
        "name": "In Progress"
      },
      "assignee": {
        "displayName": "John Doe"
      }
    }
  }
}
```

---

#### `jira_search`

Search for Jira issues using JQL (Jira Query Language).

**Parameters:**
- `jql` (string, required): JQL query
- `limit` (integer, optional, default: 50): Maximum results
- `fields` (string, optional): Comma-separated fields

**Example:**
```json
{
  "id": "2",
  "type": "REQUEST",
  "method": "jira_search",
  "params": {
    "jql": "project = PROJ AND status = Open",
    "limit": 10
  }
}
```

**Response:**
```json
{
  "id": "2",
  "type": "RESPONSE",
  "result": {
    "success": true,
    "total": 25,
    "issues": [
      {
        "key": "PROJ-123",
        "fields": { ... }
      }
    ]
  }
}
```

---

#### `jira_get_user_profile`

Get user profile information.

**Parameters:**
- `account_id` (string, required): User account ID

**Example:**
```json
{
  "id": "3",
  "type": "REQUEST",
  "method": "jira_get_user_profile",
  "params": {
    "account_id": "5b10a2844c20165700ede21g"
  }
}
```

---

#### `jira_search_fields`

Search for available Jira fields.

**Parameters:**
- `query` (string, optional): Search term

**Example:**
```json
{
  "id": "4",
  "type": "REQUEST",
  "method": "jira_search_fields",
  "params": {
    "query": "priority"
  }
}
```

---

#### `jira_get_project_issues`

Get all issues in a project.

**Parameters:**
- `project_key` (string, required): Project key
- `limit` (integer, optional, default: 50): Maximum results

**Example:**
```json
{
  "id": "5",
  "type": "REQUEST",
  "method": "jira_get_project_issues",
  "params": {
    "project_key": "PROJ",
    "limit": 20
  }
}
```

---

#### `jira_get_transitions`

Get available transitions for an issue.

**Parameters:**
- `issue_key` (string, required): Issue key

**Example:**
```json
{
  "id": "6",
  "type": "REQUEST",
  "method": "jira_get_transitions",
  "params": {
    "issue_key": "PROJ-123"
  }
}
```

**Response:**
```json
{
  "id": "6",
  "type": "RESPONSE",
  "result": {
    "success": true,
    "transitions": [
      {
        "id": "21",
        "name": "Done",
        "to": {
          "name": "Done"
        }
      }
    ]
  }
}
```

---

#### `jira_get_worklog`

Get worklog entries for an issue.

**Parameters:**
- `issue_key` (string, required): Issue key

---

#### `jira_get_agile_boards`

List all Agile boards.

**Parameters:**
- `project_key` (string, optional): Filter by project
- `limit` (integer, optional, default: 50): Maximum results

---

#### `jira_get_board_issues`

Get issues on a board.

**Parameters:**
- `board_id` (integer, required): Board ID
- `limit` (integer, optional, default: 50): Maximum results

---

#### `jira_get_sprints_from_board`

List sprints on a board.

**Parameters:**
- `board_id` (integer, required): Board ID

---

#### `jira_get_sprint_issues`

Get issues in a sprint.

**Parameters:**
- `sprint_id` (integer, required): Sprint ID
- `limit` (integer, optional, default: 50): Maximum results

---

#### `jira_get_link_types`

Get available issue link types.

**Parameters:** None

---

#### `jira_get_project_versions`

List project versions/releases.

**Parameters:**
- `project_key` (string, required): Project key

---

#### `jira_get_all_projects`

List all accessible projects.

**Parameters:**
- `limit` (integer, optional, default: 50): Maximum results

---

#### `jira_batch_get_changelogs`

Get changelogs for multiple issues.

**Parameters:**
- `issue_keys` (array[string], required): List of issue keys

**Example:**
```json
{
  "id": "7",
  "type": "REQUEST",
  "method": "jira_batch_get_changelogs",
  "params": {
    "issue_keys": ["PROJ-123", "PROJ-456"]
  }
}
```

---

#### `jira_download_attachments`

Download issue attachments.

**Parameters:**
- `issue_key` (string, required): Issue key
- `attachment_id` (string, optional): Specific attachment ID

---

### Jira Write Operations

#### `jira_create_issue`

Create a new Jira issue.

**Parameters:**
- `project` (string, required): Project key
- `summary` (string, required): Issue summary
- `issue_type` (string, required): Issue type (e.g., "Task", "Bug")
- `description` (string, optional): Issue description
- `priority` (string, optional): Priority name
- `assignee` (string, optional): Assignee account ID
- `labels` (array[string], optional): Labels
- `custom_fields` (object, optional): Custom field values

**Example:**
```json
{
  "id": "10",
  "type": "REQUEST",
  "method": "jira_create_issue",
  "params": {
    "project": "PROJ",
    "summary": "Fix login bug",
    "issue_type": "Bug",
    "description": "Users cannot login with special characters",
    "priority": "High",
    "labels": ["security", "urgent"]
  }
}
```

**Response:**
```json
{
  "id": "10",
  "type": "RESPONSE",
  "result": {
    "success": true,
    "key": "PROJ-789",
    "id": "12345",
    "self": "https://your-domain.atlassian.net/rest/api/2/issue/12345"
  }
}
```

---

#### `jira_update_issue`

Update an existing issue.

**Parameters:**
- `issue_key` (string, required): Issue key
- `fields` (object, required): Fields to update

**Example:**
```json
{
  "id": "11",
  "type": "REQUEST",
  "method": "jira_update_issue",
  "params": {
    "issue_key": "PROJ-123",
    "fields": {
      "summary": "Updated summary",
      "priority": { "name": "High" }
    }
  }
}
```

---

#### `jira_delete_issue`

Delete an issue.

**Parameters:**
- `issue_key` (string, required): Issue key

**Example:**
```json
{
  "id": "12",
  "type": "REQUEST",
  "method": "jira_delete_issue",
  "params": {
    "issue_key": "PROJ-123"
  }
}
```

---

#### `jira_add_comment`

Add a comment to an issue.

**Parameters:**
- `issue_key` (string, required): Issue key
- `body` (string, required): Comment text

**Example:**
```json
{
  "id": "13",
  "type": "REQUEST",
  "method": "jira_add_comment",
  "params": {
    "issue_key": "PROJ-123",
    "body": "This has been fixed in the latest release"
  }
}
```

---

#### `jira_add_worklog`

Log work on an issue.

**Parameters:**
- `issue_key` (string, required): Issue key
- `time_spent` (string, required): Time (e.g., "3h 30m", "1d")
- `comment` (string, optional): Worklog comment
- `started` (string, optional): Start time (ISO 8601)

**Example:**
```json
{
  "id": "14",
  "type": "REQUEST",
  "method": "jira_add_worklog",
  "params": {
    "issue_key": "PROJ-123",
    "time_spent": "2h",
    "comment": "Fixed the bug and added tests"
  }
}
```

---

#### `jira_transition_issue`

Transition an issue to a different status.

**Parameters:**
- `issue_key` (string, required): Issue key
- `transition_id` (string, required): Transition ID (use `jira_get_transitions` to find)
- `comment` (string, optional): Transition comment

**Example:**
```json
{
  "id": "15",
  "type": "REQUEST",
  "method": "jira_transition_issue",
  "params": {
    "issue_key": "PROJ-123",
    "transition_id": "21",
    "comment": "Marking as done"
  }
}
```

---

#### `jira_create_issue_link`

Link two issues together.

**Parameters:**
- `inward_issue` (string, required): Source issue key
- `outward_issue` (string, required): Target issue key
- `link_type` (string, required): Link type name

**Example:**
```json
{
  "id": "16",
  "type": "REQUEST",
  "method": "jira_create_issue_link",
  "params": {
    "inward_issue": "PROJ-123",
    "outward_issue": "PROJ-456",
    "link_type": "Blocks"
  }
}
```

---

#### `jira_create_remote_issue_link`

Create a link to an external resource.

**Parameters:**
- `issue_key` (string, required): Issue key
- `url` (string, required): External URL
- `title` (string, required): Link title

---

#### `jira_remove_issue_link`

Remove a link between issues.

**Parameters:**
- `link_id` (string, required): Link ID

---

#### `jira_link_to_epic`

Add an issue to an epic.

**Parameters:**
- `issue_key` (string, required): Issue key
- `epic_key` (string, required): Epic key

---

#### `jira_batch_create_issues`

Create multiple issues at once.

**Parameters:**
- `issues` (array[object], required): Array of issue definitions

**Example:**
```json
{
  "id": "17",
  "type": "REQUEST",
  "method": "jira_batch_create_issues",
  "params": {
    "issues": [
      {
        "project": "PROJ",
        "summary": "Task 1",
        "issue_type": "Task"
      },
      {
        "project": "PROJ",
        "summary": "Task 2",
        "issue_type": "Task"
      }
    ]
  }
}
```

---

#### `jira_create_sprint`

Create a new sprint.

**Parameters:**
- `board_id` (integer, required): Board ID
- `name` (string, required): Sprint name
- `start_date` (string, optional): Start date (ISO 8601)
- `end_date` (string, optional): End date (ISO 8601)

---

#### `jira_update_sprint`

Update sprint details.

**Parameters:**
- `sprint_id` (integer, required): Sprint ID
- `name` (string, optional): New sprint name
- `state` (string, optional): Sprint state ("active", "closed")
- `start_date` (string, optional): Start date
- `end_date` (string, optional): End date

---

#### `jira_create_version`

Create a project version/release.

**Parameters:**
- `project` (string, required): Project key
- `name` (string, required): Version name
- `description` (string, optional): Description
- `release_date` (string, optional): Release date

---

#### `jira_batch_create_versions`

Create multiple versions at once.

**Parameters:**
- `versions` (array[object], required): Array of version definitions

---

## Confluence Tools

#### `confluence_search`

Search Confluence using CQL (Confluence Query Language).

**Parameters:**
- `cql` or `query` (string, required): CQL query or text search
- `limit` (integer, optional, default: 10): Maximum results
- `spaces_filter` (string, optional): Space keys to filter

**Example:**
```json
{
  "id": "20",
  "type": "REQUEST",
  "method": "confluence_search",
  "params": {
    "query": "API documentation",
    "limit": 5,
    "spaces_filter": "DEV,DOCS"
  }
}
```

---

#### `confluence_get_page`

Get a specific Confluence page.

**Parameters:**
- `page_id` (string, required): Page ID
- `expand` (string, optional, default: "body.storage,version"): Fields to expand

**Example:**
```json
{
  "id": "21",
  "type": "REQUEST",
  "method": "confluence_get_page",
  "params": {
    "page_id": "123456",
    "expand": "body.storage,version,space"
  }
}
```

**Response:**
```json
{
  "id": "21",
  "type": "RESPONSE",
  "result": {
    "success": true,
    "id": "123456",
    "title": "API Documentation",
    "body": {
      "storage": {
        "value": "<p>Content...</p>"
      }
    },
    "version": {
      "number": 5
    }
  }
}
```

---

#### `confluence_get_page_children`

Get child pages of a page.

**Parameters:**
- `parent_id` (string, required): Parent page ID
- `limit` (integer, optional, default: 25): Maximum results

**Example:**
```json
{
  "id": "22",
  "type": "REQUEST",
  "method": "confluence_get_page_children",
  "params": {
    "parent_id": "123456",
    "limit": 10
  }
}
```

---

#### `confluence_get_comments`

Get comments on a page.

**Parameters:**
- `page_id` (string, required): Page ID

---

#### `confluence_get_labels`

Get labels on a page.

**Parameters:**
- `page_id` (string, required): Page ID

---

#### `confluence_add_label`

Add a label to a page.

**Parameters:**
- `page_id` (string, required): Page ID
- `label` (string, required): Label name

**Example:**
```json
{
  "id": "23",
  "type": "REQUEST",
  "method": "confluence_add_label",
  "params": {
    "page_id": "123456",
    "label": "important"
  }
}
```

---

#### `confluence_create_page`

Create a new Confluence page.

**Parameters:**
- `space_key` (string, required): Space key
- `title` (string, required): Page title
- `body` (string, required): Page content (HTML)
- `parent_id` (string, optional): Parent page ID

**Example:**
```json
{
  "id": "24",
  "type": "REQUEST",
  "method": "confluence_create_page",
  "params": {
    "space_key": "DEV",
    "title": "New API Guide",
    "body": "<p>This is the content...</p>",
    "parent_id": "123456"
  }
}
```

---

#### `confluence_update_page`

Update an existing page.

**Parameters:**
- `page_id` (string, required): Page ID
- `title` (string, optional): New title
- `body` (string, optional): New content (HTML)
- `version` (integer, required): Current version number

**Example:**
```json
{
  "id": "25",
  "type": "REQUEST",
  "method": "confluence_update_page",
  "params": {
    "page_id": "123456",
    "title": "Updated API Guide",
    "body": "<p>Updated content...</p>",
    "version": 5
  }
}
```

---

#### `confluence_delete_page`

Delete a page.

**Parameters:**
- `page_id` (string, required): Page ID

**Example:**
```json
{
  "id": "26",
  "type": "REQUEST",
  "method": "confluence_delete_page",
  "params": {
    "page_id": "123456"
  }
}
```

---

#### `confluence_add_comment`

Add a comment to a page.

**Parameters:**
- `page_id` (string, required): Page ID
- `comment` (string, required): Comment text

---

#### `confluence_search_user`

Search for Confluence users.

**Parameters:**
- `query` (string, required): Search query
- `limit` (integer, optional, default: 10): Maximum results

---

## Utility Tools

#### `utils_echo`

Echo back the input (for testing).

**Parameters:**
- `message` (string, required): Message to echo

**Example:**
```json
{
  "id": "99",
  "type": "REQUEST",
  "method": "utils_echo",
  "params": {
    "message": "Hello, MCP!"
  }
}
```

**Response:**
```json
{
  "id": "99",
  "type": "RESPONSE",
  "result": {
    "success": true,
    "message": "Hello, MCP!"
  }
}
```

---

## Error Responses

All tools follow a consistent error response format:

```json
{
  "id": "1",
  "type": "ERROR",
  "error": "Missing required parameter: issue_key"
}
```

or

```json
{
  "id": "1",
  "type": "RESPONSE",
  "result": {
    "success": false,
    "error": "Issue PROJ-123 not found"
  }
}
```

---

## Common Parameters

### Pagination

Most list operations support:
- `limit` (integer): Maximum results (default varies by tool)
- `start_at` (integer, optional): Starting index for pagination

### Field Selection

Many read operations support:
- `fields` (string): Comma-separated field names
- `expand` (string): Additional data to include

---

## Rate Limiting

Respect Atlassian's API rate limits:
- **Cloud:** 100-200 requests per minute per user
- **Data Center:** Varies by configuration

The server does not implement rate limiting; it relies on Atlassian's enforcement.

---

## Best Practices

1. **Use Field Selection**: Request only needed fields to reduce response size
2. **Batch Operations**: Use batch tools when creating/updating multiple items
3. **Pagination**: Use `limit` and `start_at` for large result sets
4. **Error Handling**: Always check `success` field in responses
5. **JQL Optimization**: Use indexed fields in JQL queries for better performance

---

**See Also:**
- [Authentication Guide](AUTHENTICATION.md)
- [Quick Start](../README.md#quick-start)
- [Atlassian REST API Docs](https://developer.atlassian.com/cloud/jira/platform/rest/v3/)
