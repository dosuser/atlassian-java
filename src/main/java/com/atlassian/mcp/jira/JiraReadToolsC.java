package com.atlassian.mcp.jira;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Jira Read Tools - Group C (S-Z).
 * getUserProfile, getWorklog, search, searchFields
 */
public class JiraReadToolsC {
    private final Supplier<JiraClient> clientSupplier;

    public JiraReadToolsC(Supplier<JiraClient> clientSupplier) {
        this.clientSupplier = clientSupplier;
    }
    
    private JiraClient getClient() {
        return clientSupplier.get();
    }

    /**
     * Retrieve profile information for a specific Jira user (jira_get_user_profile).
     * 
     * Python 스키마:
     * - user_identifier (string, required): User identifier (email, username, key, or account ID)
     */
    public Mono<Map<String, Object>> getUserProfile(Map<String, Object> params) {
        String userIdentifier = (String) params.get("user_identifier");
        if (userIdentifier == null || userIdentifier.isBlank()) {
            return Mono.error(new IllegalArgumentException("user_identifier is required"));
        }
        return getClient().getUserProfile(userIdentifier)
                .map(node -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("accountId", node.path("accountId").asText());
                    result.put("displayName", node.path("displayName").asText());
                    result.put("emailAddress", node.path("emailAddress").asText());
                    return result;
                })
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Get worklog entries for a Jira issue (jira_get_worklog).
     * 
     * Python 스키마:
     * - issue_key (string, required): Jira issue key
     */
    public Mono<Map<String, Object>> getWorklog(Map<String, Object> params) {
        String issueKey = (String) params.get("issue_key");
        if (issueKey == null || issueKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("issue_key is required"));
        }
        return getClient().getWorklogs(issueKey)
                .map(node -> Map.<String, Object>of(
                        "success", true,
                        "worklogs", node.path("worklogs")
                ))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Search Jira issues using JQL (jira_search).
     * 
     * Python 스키마:
     * - jql (string, required): JQL query string
     * - fields (string, default: summary,description,...): Comma-separated fields or '*all'
     * - limit (integer, default: 10): Maximum number of results (1-50)
     * - start_at (integer, default: 0): Starting index for pagination
     * - projects_filter (string, optional): Comma-separated project keys
     * - expand (string, optional): Fields to expand (renderedFields, transitions, changelog)
     */
    public Mono<Map<String, Object>> search(Map<String, Object> params) {
        String jql = (String) params.get("jql");
        String fields = (String) params.getOrDefault("fields", "summary,status,assignee,created");
        int limit = (int) params.getOrDefault("limit", 10);
        int startAt = (int) params.getOrDefault("start_at", 0);
        String expand = (String) params.get("expand");

        if (jql == null || jql.isBlank()) {
            return Mono.error(new IllegalArgumentException("jql is required"));
        }

        return getClient().searchIssues(jql, fields, startAt, limit, expand)
                .map(this::convertSearchResults)
                .onErrorResume(e -> Mono.just(Map.of(
                        "success", false,
                        "error", e.getMessage(),
                        "jql", jql
                )));
    }

    /**
     * Search Jira fields by keyword with fuzzy match (jira_search_fields).
     * 
     * Python 스키마:
     * - keyword (string, default: ""): Keyword for fuzzy search
     * - limit (integer, default: 10): Maximum number of results
     * - refresh (boolean, default: false): Force refresh the field list
     */
    public Mono<Map<String, Object>> searchFields(Map<String, Object> params) {
        String keyword = (String) params.getOrDefault("keyword", "");
        int limit = (int) params.getOrDefault("limit", 10);
        
        return getClient().getFields()
                .map(fields -> {
                    var results = new java.util.ArrayList<Map<String, Object>>();
                    fields.forEach(field -> {
                        String name = field.path("name").asText().toLowerCase();
                        String id = field.path("id").asText().toLowerCase();
                        if (keyword.isBlank() || name.contains(keyword.toLowerCase()) || id.contains(keyword.toLowerCase())) {
                            results.add(Map.of(
                                    "id", field.path("id").asText(),
                                    "name", field.path("name").asText(),
                                    "custom", field.path("custom").asBoolean()
                            ));
                        }
                    });
                    return Map.<String, Object>of(
                            "success", true,
                            "fields", results.stream().limit(limit).toList()
                    );
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
