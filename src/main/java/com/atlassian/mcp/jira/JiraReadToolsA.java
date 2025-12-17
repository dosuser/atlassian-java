package com.atlassian.mcp.jira;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Jira Read Tools - Group A (A-G).
 * batchGetChangelogs, downloadAttachments, getAgileBoards, getAllProjects, getBoardIssues, getIssue
 */
public class JiraReadToolsA {
    private final Supplier<JiraClient> clientSupplier;

    public JiraReadToolsA(Supplier<JiraClient> clientSupplier) {
        this.clientSupplier = clientSupplier;
    }
    
    private JiraClient getClient() {
        return clientSupplier.get();
    }

    /**
     * Batch get changelogs for multiple Jira issues (jira_batch_get_changelogs).
     * 
     * Python 스키마:
     * - issue_ids_or_keys (array, required): List of issue IDs or keys
     * - fields (array, optional): Filter changelogs by fields
     * - limit (integer, default: -1): Maximum changelogs per issue
     */
    public Mono<Map<String, Object>> batchGetChangelogs(Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        java.util.List<String> issueKeys = (java.util.List<String>) params.get("issue_ids_or_keys");
        
        if (issueKeys == null || issueKeys.isEmpty()) {
            return Mono.error(new IllegalArgumentException("issue_ids_or_keys is required"));
        }
        
        return getClient().batchGetChangelogs(issueKeys)
                .map(changelogs -> {
                    var results = new java.util.ArrayList<Map<String, Object>>();
                    changelogs.forEach(issue -> {
                        Map<String, Object> issueData = new HashMap<>();
                        issueData.put("id", issue.path("id").asText());
                        issueData.put("key", issue.path("key").asText());
                        
                        var histories = new java.util.ArrayList<Map<String, Object>>();
                        issue.path("changelog").path("histories").forEach(h -> {
                            histories.add(Map.of(
                                    "id", h.path("id").asText(),
                                    "created", h.path("created").asText(),
                                    "author", h.path("author").path("displayName").asText()
                            ));
                        });
                        issueData.put("changelogs", histories);
                        results.add(issueData);
                    });
                    return Map.<String, Object>of("success", true, "issues", results);
                })
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Download attachments from a Jira issue (jira_download_attachments).
     * 
     * Python 스키마:
     * - issue_key (string, required): Jira issue key
     * - target_dir (string, required): Directory to save attachments
     */
    public Mono<Map<String, Object>> downloadAttachments(Map<String, Object> params) {
        String issueKey = (String) params.get("issue_key");
        String targetDir = (String) params.get("target_dir");
        
        if (issueKey == null || targetDir == null) {
            return Mono.error(new IllegalArgumentException("issue_key and target_dir are required"));
        }
        
        // TODO: 파일 다운로드 구현 필요
        return Mono.just(Map.<String, Object>of(
                "success", false,
                "error", "File download not implemented in this version. Use direct REST API calls."
        ));
    }

    /**
     * Get jira agile boards (jira_get_agile_boards).
     * 
     * Python 스키마:
     * - board_name (string, optional): Name of the board (fuzzy search)
     * - project_key (string, optional): Project key
     * - board_type (string, optional): Board type ('scrum' or 'kanban')
     * - start_at (integer, default: 0): Starting index
     * - limit (integer, default: 10): Maximum results (1-50)
     */
    public Mono<Map<String, Object>> getAgileBoards(Map<String, Object> params) {
        String boardName = (String) params.get("board_name");
        String projectKey = (String) params.get("project_key");
        String boardType = (String) params.get("board_type");
        int startAt = (int) params.getOrDefault("start_at", 0);
        int limit = (int) params.getOrDefault("limit", 10);
        
        return getClient().getAgileBoards(boardName, projectKey, boardType, startAt, limit)
                .map(node -> {
                    var boards = new java.util.ArrayList<Map<String, Object>>();
                    node.path("values").forEach(b -> boards.add(Map.of(
                            "id", b.path("id").asText(),
                            "name", b.path("name").asText(),
                            "type", b.path("type").asText()
                    )));
                    return Map.<String, Object>of("success", true, "boards", boards);
                })
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Get all Jira projects (jira_get_all_projects).
     * 
     * Python 스키마:
     * - include_archived (boolean, default: false): Include archived projects
     */
    public Mono<Map<String, Object>> getAllProjects(Map<String, Object> params) {
        return getClient().getAllProjects()
                .map(projects -> {
                    var projectList = new java.util.ArrayList<Map<String, Object>>();
                    projects.forEach(p -> projectList.add(Map.of(
                            "id", p.path("id").asText(),
                            "key", p.path("key").asText(),
                            "name", p.path("name").asText()
                    )));
                    return Map.<String, Object>of("success", true, "projects", projectList);
                })
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Get all issues linked to a specific board (jira_get_board_issues).
     * 
     * Python 스키마:
     * - board_id (string, required): The ID of the board
     * - jql (string, required): JQL query string
     * - fields (string, default: summary,description,...): Comma-separated fields
     * - start_at (integer, default: 0): Starting index
     * - limit (integer, default: 10): Maximum results (1-50)
     * - expand (string, default: version): Fields to expand
     */
    public Mono<Map<String, Object>> getBoardIssues(Map<String, Object> params) {
        Object boardIdObj = params.get("board_id");
        String boardId = boardIdObj instanceof Integer ? String.valueOf(boardIdObj) : (String) boardIdObj;
        String jql = (String) params.get("jql");
        String fields = (String) params.getOrDefault("fields", "summary,status");
        int startAt = (int) params.getOrDefault("start_at", 0);
        int limit = (int) params.getOrDefault("limit", 10);
        String expand = (String) params.get("expand");
        
        if (boardId == null || boardId.isBlank()) {
            return Mono.error(new IllegalArgumentException("board_id is required"));
        }
        
        return getClient().getBoardIssues(boardId, jql, fields, startAt, limit, expand)
                .map(this::convertSearchResults)
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Get details of a specific Jira issue (jira_get_issue).
     * 
     * Python 스키마:
     * - issue_key (string, required): Jira issue key
     * - fields (string, default: summary,description,...): Comma-separated fields or '*all' for all
     * - expand (string, optional): Fields to expand (renderedFields, transitions, changelog)
     * - comment_limit (integer, default: 10): Maximum number of comments (0-100)
     * - properties (string, optional): Comma-separated issue properties
     * - update_history (boolean, default: true): Update issue view history
     */
    public Mono<Map<String, Object>> getIssue(Map<String, Object> params) {
        String issueKey = (String) params.get("issue_key");
        String fields = (String) params.getOrDefault("fields", "summary,status,assignee,reporter,created,updated");
        String expand = (String) params.get("expand");
        
        if (issueKey == null || issueKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("issue_key is required"));
        }

        return getClient().getIssue(issueKey, fields, expand)
                .map(this::convertToSimplified)
                .onErrorResume(e -> Mono.just(Map.of(
                        "success", false,
                        "error", e.getMessage(),
                        "issue_key", issueKey
                )));
    }

    // Helper methods
    private Map<String, Object> convertToSimplified(JsonNode node) {
        Map<String, Object> result = new HashMap<>();
        result.put("key", node.path("key").asText());
        result.put("id", node.path("id").asText());
        
        JsonNode fields = node.path("fields");
        Map<String, Object> fieldsMap = new HashMap<>();
        fieldsMap.put("summary", fields.path("summary").asText());
        fieldsMap.put("status", fields.path("status").path("name").asText());
        fieldsMap.put("assignee", fields.path("assignee").path("displayName").asText());
        fieldsMap.put("reporter", fields.path("reporter").path("displayName").asText());
        fieldsMap.put("created", fields.path("created").asText());
        fieldsMap.put("updated", fields.path("updated").asText());
        
        result.put("fields", fieldsMap);
        result.put("success", true);
        return result;
    }

    private Map<String, Object> convertSearchResults(JsonNode node) {
        Map<String, Object> result = new HashMap<>();
        result.put("total", node.path("total").asInt());
        result.put("startAt", node.path("startAt").asInt());
        result.put("maxResults", node.path("maxResults").asInt());
        
        var issues = new java.util.ArrayList<Map<String, Object>>();
        node.path("issues").forEach(issue -> issues.add(convertToSimplified(issue)));
        
        result.put("issues", issues);
        result.put("success", true);
        return result;
    }
}
