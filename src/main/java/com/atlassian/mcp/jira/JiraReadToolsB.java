package com.atlassian.mcp.jira;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Jira Read Tools - Group B (G-S).
 * getLinkTypes, getProjectIssues, getProjectVersions, getSprintIssues, getSprintsFromBoard, getTransitions
 */
public class JiraReadToolsB {
    private final Supplier<JiraClient> clientSupplier;

    public JiraReadToolsB(Supplier<JiraClient> clientSupplier) {
        this.clientSupplier = clientSupplier;
    }
    
    private JiraClient getClient() {
        return clientSupplier.get();
    }

    /**
     * Get all available issue link types (jira_get_link_types).
     * 
     * Python 스키마:
     * (No parameters)
     */
    public Mono<Map<String, Object>> getLinkTypes(Map<String, Object> params) {
        return getClient().getIssueLinkTypes()
                .map(node -> {
                    var linkTypes = new java.util.ArrayList<Map<String, Object>>();
                    node.path("issueLinkTypes").forEach(lt -> linkTypes.add(Map.of(
                            "id", lt.path("id").asText(),
                            "name", lt.path("name").asText(),
                            "inward", lt.path("inward").asText(),
                            "outward", lt.path("outward").asText()
                    )));
                    return Map.<String, Object>of("success", true, "linkTypes", linkTypes);
                })
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Get all issues for a specific Jira project (jira_get_project_issues).
     * 
     * Python 스키마:
     * - project_key (string, required): The project key
     * - limit (integer, default: 10): Maximum number of results (1-50)
     * - start_at (integer, default: 0): Starting index for pagination
     */
    public Mono<Map<String, Object>> getProjectIssues(Map<String, Object> params) {
        String projectKey = (String) params.get("project_key");
        int limit = (int) params.getOrDefault("limit", 10);
        int startAt = (int) params.getOrDefault("start_at", 0);
        
        if (projectKey == null || projectKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("project_key is required"));
        }
        
        String jql = "project=" + projectKey;
        return getClient().searchIssues(jql, "summary,status,assignee", startAt, limit, null)
                .map(this::convertSearchResults)
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Get all fix versions for a specific Jira project (jira_get_project_versions).
     * 
     * Python 스키마:
     * - project_key (string, required): Jira project key
     */
    public Mono<Map<String, Object>> getProjectVersions(Map<String, Object> params) {
        String projectKey = (String) params.get("project_key");
        if (projectKey == null || projectKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("project_key is required"));
        }
        return getClient().getProjectVersions(projectKey)
                .map(versions -> Map.<String, Object>of("success", true, "versions", versions))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Get jira issues from sprint (jira_get_sprint_issues).
     * 
     * Python 스키마:
     * - sprint_id (string, required): The ID of the sprint
     * - fields (string, default: summary,description,...): Comma-separated fields
     * - start_at (integer, default: 0): Starting index for pagination
     * - limit (integer, default: 10): Maximum number of results (1-50)
     */
    public Mono<Map<String, Object>> getSprintIssues(Map<String, Object> params) {
        Object sprintIdObj = params.get("sprint_id");
        String sprintId = sprintIdObj instanceof Integer ? String.valueOf(sprintIdObj) : (String) sprintIdObj;
        String fields = (String) params.getOrDefault("fields", "summary,status");
        int startAt = (int) params.getOrDefault("start_at", 0);
        int limit = (int) params.getOrDefault("limit", 10);
        
        if (sprintId == null || sprintId.isBlank()) {
            return Mono.error(new IllegalArgumentException("sprint_id is required"));
        }
        
        return getClient().getSprintIssues(sprintId, fields, startAt, limit)
                .map(this::convertSearchResults)
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Get jira sprints from board (jira_get_sprints_from_board).
     * 
     * Python 스키마:
     * - board_id (string, required): The ID of the board
     * - state (string, optional): Sprint state (active, future, closed)
     * - start_at (integer, default: 0): Starting index for pagination
     * - limit (integer, default: 10): Maximum number of results (1-50)
     */
    public Mono<Map<String, Object>> getSprintsFromBoard(Map<String, Object> params) {
        Object boardIdObj = params.get("board_id");
        String boardId = boardIdObj instanceof Integer ? String.valueOf(boardIdObj) : (String) boardIdObj;
        String state = (String) params.get("state");
        int startAt = (int) params.getOrDefault("start_at", 0);
        int limit = (int) params.getOrDefault("limit", 10);
        
        if (boardId == null || boardId.isBlank()) {
            return Mono.error(new IllegalArgumentException("board_id is required"));
        }
        
        return getClient().getSprintsFromBoard(boardId, state, startAt, limit)
                .map(node -> {
                    var sprints = new java.util.ArrayList<Map<String, Object>>();
                    node.path("values").forEach(s -> sprints.add(Map.of(
                            "id", s.path("id").asText(),
                            "name", s.path("name").asText(),
                            "state", s.path("state").asText()
                    )));
                    return Map.<String, Object>of("success", true, "sprints", sprints);
                })
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Get available status transitions for a Jira issue (jira_get_transitions).
     * 
     * Python 스키마:
     * - issue_key (string, required): Jira issue key
     */
    public Mono<Map<String, Object>> getTransitions(Map<String, Object> params) {
        String issueKey = (String) params.get("issue_key");
        if (issueKey == null || issueKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("issue_key is required"));
        }
        return getClient().getTransitions(issueKey)
                .map(node -> {
                    var transitions = new java.util.ArrayList<Map<String, Object>>();
                    node.path("transitions").forEach(t -> transitions.add(Map.of(
                            "id", t.path("id").asText(),
                            "name", t.path("name").asText(),
                            "to", Map.of("name", t.path("to").path("name").asText())
                    )));
                    return Map.<String, Object>of("success", true, "transitions", transitions);
                })
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
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
