package com.atlassian.mcp.server;

import com.atlassian.mcp.core.ToolRegistry;
import com.atlassian.mcp.jira.JiraReadToolsA;
import com.atlassian.mcp.jira.JiraReadToolsB;
import com.atlassian.mcp.jira.JiraReadToolsC;
import com.atlassian.mcp.jira.JiraWriteTools;

import java.util.Map;

/**
 * Jira 도구 등록 설정
 */
public class JiraToolsConfig {
    
    public static void configure(ToolRegistry reg, 
                                 JiraReadToolsA readToolsA,
                                 JiraReadToolsB readToolsB,
                                 JiraReadToolsC readToolsC,
                                 JiraWriteTools writeTools) {
        // Jira 읽기 도구 - Group C (S-Z)
        reg.register(
            "jira_get_user_profile",
            "Retrieve profile information for a specific Jira user.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "user_identifier", Map.of("type", "string", "description", "Identifier for the user (e.g., email address 'user@example.com', username 'johndoe', account ID 'accountid:...', or key for Server/DC).")
                ),
                "required", java.util.List.of("user_identifier")
            ),
            params -> readToolsC.getUserProfile(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_get_issue",
            "Get details of a specific Jira issue including its Epic links and relationship information.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "issue_key", Map.of("type", "string", "description", "Jira issue key (e.g., 'PROJ-123')"),
                    "fields", Map.of("type", "string", "description", "Comma-separated fields to return or '*all' for all fields"),
                    "expand", Map.of("type", "string", "description", "Fields to expand (e.g., 'renderedFields', 'transitions')"),
                    "comment_limit", Map.of("type", "integer", "description", "Maximum number of comments", "default", 10),
                    "properties", Map.of("type", "string", "description", "Comma-separated issue properties"),
                    "update_history", Map.of("type", "boolean", "description", "Whether to update issue view history", "default", true)
                ),
                "required", java.util.List.of("issue_key")
            ),
            params -> readToolsA.getIssue(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_search",
            "Search Jira issues using JQL (Jira Query Language).",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "jql", Map.of("type", "string", "description", "JQL query string"),
                    "fields", Map.of("type", "string", "description", "Comma-separated fields to return"),
                    "limit", Map.of("type", "integer", "description", "Maximum number of results (1-50)", "default", 10),
                    "start_at", Map.of("type", "integer", "description", "Starting index for pagination", "default", 0),
                    "projects_filter", Map.of("type", "string", "description", "Comma-separated project keys to filter"),
                    "expand", Map.of("type", "string", "description", "Fields to expand")
                ),
                "required", java.util.List.of("jql")
            ),
            params -> readToolsC.search(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_search_fields",
            "Search Jira fields by keyword with fuzzy match.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "keyword", Map.of("type", "string", "description", "Keyword for fuzzy search. If left empty, lists the first 'limit' available fields in their default order.", "default", ""),
                    "limit", Map.of("type", "integer", "description", "Maximum number of results", "default", 10),
                    "refresh", Map.of("type", "boolean", "description", "Whether to force refresh the field list", "default", false)
                )
            ),
            params -> readToolsC.searchFields(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_get_project_issues",
            "Get all issues for a specific Jira project.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "project_key", Map.of("type", "string", "description", "The project key"),
                    "limit", Map.of("type", "integer", "description", "Maximum number of results (1-50)", "default", 10),
                    "start_at", Map.of("type", "integer", "description", "Starting index for pagination (0-based)", "default", 0)
                ),
                "required", java.util.List.of("project_key")
            ),
            params -> readToolsB.getProjectIssues(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_get_transitions",
            "Get available status transitions for a Jira issue.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "issue_key", Map.of("type", "string", "description", "Jira issue key (e.g., 'PROJ-123')")
                ),
                "required", java.util.List.of("issue_key")
            ),
            params -> readToolsB.getTransitions(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_get_worklog",
            "Get worklog entries for a Jira issue.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "issue_key", Map.of("type", "string", "description", "Jira issue key (e.g., 'PROJ-123')")
                ),
                "required", java.util.List.of("issue_key")
            ),
            params -> readToolsC.getWorklog(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_download_attachments",
            "Download attachments from a Jira issue.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "issue_key", Map.of("type", "string", "description", "Jira issue key (e.g., 'PROJ-123')"),
                    "target_dir", Map.of("type", "string", "description", "Directory where attachments should be saved")
                ),
                "required", java.util.List.of("issue_key", "target_dir")
            ),
            params -> readToolsA.downloadAttachments(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_get_agile_boards",
            "Get jira agile boards by name, project key, or type.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "board_name", Map.of("type", "string", "description", "(Optional) The name of board, support fuzzy search"),
                    "project_key", Map.of("type", "string", "description", "(Optional) Jira project key (e.g., 'PROJ-123')"),
                    "board_type", Map.of("type", "string", "description", "(Optional) The type of jira board (e.g., 'scrum', 'kanban')"),
                    "start_at", Map.of("type", "integer", "description", "Starting index for pagination (0-based)", "default", 0),
                    "limit", Map.of("type", "integer", "description", "Maximum number of results", "default", 50)
                )
            ),
            params -> readToolsA.getAgileBoards(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_get_board_issues",
            "Get all issues linked to a specific board filtered by JQL.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "board_id", Map.of("type", "integer", "description", "The ID of the board"),
                    "start_at", Map.of("type", "integer", "description", "Starting index for pagination (0-based)", "default", 0),
                    "limit", Map.of("type", "integer", "description", "Maximum number of results", "default", 50)
                ),
                "required", java.util.List.of("board_id")
            ),
            params -> readToolsA.getBoardIssues(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_get_sprints_from_board",
            "Get jira sprints from board by state.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "board_id", Map.of("type", "integer", "description", "The ID of the board"),
                    "state", Map.of("type", "string", "description", "(Optional) Filter sprints by state (e.g., 'active', 'closed', 'future')"),
                    "start_at", Map.of("type", "integer", "description", "Starting index for pagination (0-based)", "default", 0),
                    "limit", Map.of("type", "integer", "description", "Maximum number of results", "default", 50)
                ),
                "required", java.util.List.of("board_id")
            ),
            params -> readToolsB.getSprintsFromBoard(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_get_sprint_issues",
            "Get jira issues from sprint.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "sprint_id", Map.of("type", "integer", "description", "The ID of the sprint"),
                    "start_at", Map.of("type", "integer", "description", "Starting index for pagination (0-based)", "default", 0),
                    "limit", Map.of("type", "integer", "description", "Maximum number of results", "default", 50)
                ),
                "required", java.util.List.of("sprint_id")
            ),
            params -> readToolsB.getSprintIssues(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_get_link_types",
            "Get all available issue link types.",
            Map.of("type", "object", "properties", Map.of()),
            params -> readToolsB.getLinkTypes(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_get_project_versions",
            "Get all fix versions for a specific Jira project.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "project_key", Map.of("type", "string", "description", "Project key")
                ),
                "required", java.util.List.of("project_key")
            ),
            params -> readToolsB.getProjectVersions(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_get_all_projects",
            "Get all Jira projects accessible to the current user. Project keys are always returned in uppercase. If JIRA_PROJECTS_FILTER is configured, only returns projects matching those keys.",
            Map.of("type", "object", "properties", Map.of()),
            params -> readToolsA.getAllProjects(asMap(params)).toFuture().join()
        );
        
        // Jira 쓰기 도구
        reg.register(
            "jira_create_issue",
            "Create a new Jira issue with optional Epic link or parent for subtasks.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "project_key", Map.of("type", "string", "description", "Project key (e.g., 'PROJ')"),
                    "summary", Map.of("type", "string", "description", "Issue summary/title"),
                    "issue_type", Map.of("type", "string", "description", "Issue type (Task, Bug, Story, Epic, Subtask)"),
                    "assignee", Map.of("type", "string", "description", "Assignee email, name, or account ID"),
                    "description", Map.of("type", "string", "description", "Issue description"),
                    "components", Map.of("type", "string", "description", "Comma-separated component names"),
                    "additional_fields", Map.of("type", "object", "description", "Additional fields (priority, labels, parent, etc.)")
                ),
                "required", java.util.List.of("project_key", "summary", "issue_type")
            ),
            false,
            params -> writeTools.createIssue(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_batch_create_issues",
            "Create multiple Jira issues in a batch.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "issues", Map.of(
                        "type", "string",
                        "description", "JSON array of issue objects. Each object should contain: project_key (required), summary (required), issue_type (required), description (optional), assignee (optional), components (optional). Example: [{\"project_key\": \"PROJ\", \"summary\": \"Issue 1\", \"issue_type\": \"Task\"}, {\"project_key\": \"PROJ\", \"summary\": \"Issue 2\", \"issue_type\": \"Bug\", \"components\": [\"Frontend\"]}]"
                    ),
                    "validate_only", Map.of(
                        "type", "boolean",
                        "description", "If true, only validates the issues without creating them",
                        "default", false
                    )
                ),
                "required", java.util.List.of("issues")
            ),
            false,
            params -> writeTools.batchCreateIssues(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_batch_get_changelogs",
            "Get changelogs for multiple Jira issues (Cloud only).",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "issue_ids_or_keys", Map.of("type", "array", "items", Map.of("type", "string"), "description", "List of Jira issue IDs or keys, e.g. ['PROJ-123', 'PROJ-124']"),
                    "fields", Map.of("type", "array", "items", Map.of("type", "string"), "description", "(Optional) Filter the changelogs by fields, e.g. ['status', 'assignee']. Default to None for all fields."),
                    "limit", Map.of("type", "integer", "description", "Maximum number of changelogs to return in result for each issue. Default to -1 for all changelogs. Notice that it only limits the results in the response, the function will still fetch all the data.", "default", -1)
                ),
                "required", java.util.List.of("issue_ids_or_keys")
            ),
            params -> readToolsA.batchGetChangelogs(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_update_issue",
            "Update an existing Jira issue including changing status, adding Epic links, updating fields, etc.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "issue_key", Map.of("type", "string", "description", "Jira issue key (e.g., 'PROJ-123')"),
                    "fields", Map.of("type", "object", "description", "Dictionary of fields to update. For 'assignee', provide a string identifier (email, name, or accountId). Example: `{'assignee': 'user@example.com', 'summary': 'New Summary'}`"),
                    "additional_fields", Map.of("type", "object", "description", "(Optional) Dictionary of additional fields to update. Use this for custom fields or more complex updates."),
                    "attachments", Map.of("type", "string", "description", "(Optional) JSON string array or comma-separated list of file paths to attach to the issue. Example: '/path/to/file1.txt,/path/to/file2.txt' or ['/path/to/file1.txt','/path/to/file2.txt']")
                ),
                "required", java.util.List.of("issue_key", "fields")
            ),
            false,
            params -> writeTools.updateIssue(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_delete_issue",
            "Delete an existing Jira issue.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "issue_key", Map.of("type", "string", "description", "Jira issue key (e.g., 'PROJ-123')")
                ),
                "required", java.util.List.of("issue_key")
            ),
            false,
            params -> writeTools.deleteIssue(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_add_comment",
            "Add a comment to a Jira issue.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "issue_key", Map.of("type", "string", "description", "Jira issue key (e.g., 'PROJ-123')"),
                    "comment", Map.of("type", "string", "description", "Comment text")
                ),
                "required", java.util.List.of("issue_key", "comment")
            ),
            false,
            params -> writeTools.addComment(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_add_worklog",
            "Add a worklog entry to a Jira issue.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "issue_key", Map.of("type", "string", "description", "Jira issue key (e.g., 'PROJ-123')"),
                    "time_spent", Map.of("type", "string", "description", "Time spent in Jira format. Examples: '1h 30m' (1 hour and 30 minutes), '1d' (1 day), '30m' (30 minutes), '4h' (4 hours)"),
                    "comment", Map.of("type", "string", "description", "(Optional) Comment for the worklog in Markdown format"),
                    "started", Map.of("type", "string", "description", "(Optional) Start time in ISO format. If not provided, the current time will be used. Example: '2023-08-01T12:00:00.000+0000'"),
                    "original_estimate", Map.of("type", "string", "description", "(Optional) New value for the original estimate"),
                    "remaining_estimate", Map.of("type", "string", "description", "(Optional) New value for the remaining estimate")
                ),
                "required", java.util.List.of("issue_key", "time_spent")
            ),
            false,
            params -> writeTools.addWorklog(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_link_to_epic",
            "Link an existing issue to an epic.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "issue_key", Map.of("type", "string", "description", "The key of the issue to link (e.g., 'PROJ-123')"),
                    "epic_key", Map.of("type", "string", "description", "The key of the epic to link to (e.g., 'PROJ-456')")
                ),
                "required", java.util.List.of("issue_key", "epic_key")
            ),
            false,
            params -> writeTools.linkToEpic(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_create_issue_link",
            "Create a link between two Jira issues.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "inward_issue_key", Map.of("type", "string", "description", "Inward issue key (e.g., 'PROJ-123')"),
                    "outward_issue_key", Map.of("type", "string", "description", "Outward issue key (e.g., 'PROJ-456')"),
                    "link_type", Map.of("type", "string", "description", "Link type name (e.g., 'Blocks', 'Relates')")
                ),
                "required", java.util.List.of("inward_issue_key", "outward_issue_key", "link_type")
            ),
            false,
            params -> writeTools.createIssueLink(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_create_remote_issue_link",
            "Create a remote issue link (web link or Confluence link) for a Jira issue. This tool allows you to add web links and Confluence links to Jira issues. The links will appear in the issue's Links section and can be clicked to navigate to external resources.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "issue_key", Map.of("type", "string", "description", "Jira issue key (e.g., 'PROJ-123')"),
                    "url", Map.of("type", "string", "description", "Remote URL"),
                    "title", Map.of("type", "string", "description", "(Optional) Link title"),
                    "summary", Map.of("type", "string", "description", "(Optional) Link summary")
                ),
                "required", java.util.List.of("issue_key", "url")
            ),
            false,
            params -> writeTools.createRemoteIssueLink(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_remove_issue_link",
            "Remove a link between two Jira issues.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "link_id", Map.of("type", "string", "description", "Issue link ID")
                ),
                "required", java.util.List.of("link_id")
            ),
            false,
            params -> writeTools.removeIssueLink(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_transition_issue",
            "Transition a Jira issue to a new status.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "issue_key", Map.of("type", "string", "description", "Jira issue key (e.g., 'PROJ-123')"),
                    "transition_id", Map.of("type", "string", "description", "ID of the transition to perform. Use the jira_get_transitions tool first to get the available transition IDs for the issue. Example values: '11', '21', '31'"),
                    "fields", Map.of("type", "object", "description", "(Optional) Dictionary of fields to update during the transition. Some transitions require specific fields to be set (e.g., resolution). Example: {'resolution': {'name': 'Fixed'}}"),
                    "comment", Map.of("type", "string", "description", "(Optional) Comment to add during the transition. This will be visible in the issue history.")
                ),
                "required", java.util.List.of("issue_key", "transition_id")
            ),
            false,
            params -> writeTools.transitionIssue(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_create_sprint",
            "Create Jira sprint for a board.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "board_id", Map.of("type", "string", "description", "The id of board (e.g., '1000')"),
                    "sprint_name", Map.of("type", "string", "description", "Name of the sprint (e.g., 'Sprint 1')"),
                    "start_date", Map.of("type", "string", "description", "Start time for sprint (ISO 8601 format)"),
                    "end_date", Map.of("type", "string", "description", "End time for sprint (ISO 8601 format)"),
                    "goal", Map.of("type", "string", "description", "(Optional) Goal of the sprint")
                ),
                "required", java.util.List.of("board_id", "sprint_name", "start_date", "end_date")
            ),
            false,
            params -> writeTools.createSprint(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_update_sprint",
            "Update jira sprint.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "sprint_id", Map.of("type", "string", "description", "The id of sprint (e.g., '10001')"),
                    "sprint_name", Map.of("type", "string", "description", "(Optional) New name for the sprint"),
                    "state", Map.of("type", "string", "description", "(Optional) New state for the sprint (future|active|closed)"),
                    "start_date", Map.of("type", "string", "description", "(Optional) New start date for the sprint"),
                    "end_date", Map.of("type", "string", "description", "(Optional) New end date for the sprint"),
                    "goal", Map.of("type", "string", "description", "(Optional) New goal for the sprint")
                ),
                "required", java.util.List.of("sprint_id")
            ),
            false,
            params -> writeTools.updateSprint(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_create_version",
            "Create a new fix version in a Jira project.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "project_key", Map.of("type", "string", "description", "Jira project key (e.g., 'PROJ')"),
                    "name", Map.of("type", "string", "description", "Name of the version"),
                    "start_date", Map.of("type", "string", "description", "Start date (YYYY-MM-DD)"),
                    "release_date", Map.of("type", "string", "description", "Release date (YYYY-MM-DD)"),
                    "description", Map.of("type", "string", "description", "Description of the version")
                ),
                "required", java.util.List.of("project_key", "name")
            ),
            false,
            params -> writeTools.createVersion(asMap(params)).toFuture().join()
        );
        reg.register(
            "jira_batch_create_versions",
            "Batch create multiple versions in a Jira project.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "project_key", Map.of("type", "string", "description", "Jira project key (e.g., 'PROJ')"),
                    "versions", Map.of(
                        "type", "string",
                        "description", "JSON array of version objects. Each object should contain: name (required), startDate (optional, YYYY-MM-DD), releaseDate (optional, YYYY-MM-DD), description (optional). Example: [{\"name\": \"v1.0\", \"startDate\": \"2025-01-01\", \"releaseDate\": \"2025-02-01\", \"description\": \"First release\"}, {\"name\": \"v2.0\"}]"
                    )
                ),
                "required", java.util.List.of("project_key", "versions")
            ),
            false,
            params -> writeTools.batchCreateVersions(asMap(params)).toFuture().join()
        );
    }
    
    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object params) {
        return params instanceof Map ? (Map<String, Object>) params : Map.of();
    }
}
