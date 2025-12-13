package com.atlassian.mcp.jira;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Jira Write Tools (Create/Update/Delete operations).
 * addComment, addWorklog, batchCreateIssues, batchCreateVersions, createIssue, createIssueLink,
 * createRemoteIssueLink, createSprint, createVersion, deleteIssue, linkToEpic,
 * removeIssueLink, transitionIssue, updateIssue, updateSprint
 */
public class JiraWriteTools {
    private final Supplier<JiraClient> clientSupplier;

    public JiraWriteTools(Supplier<JiraClient> clientSupplier) {
        this.clientSupplier = clientSupplier;
    }
    
    private JiraClient getClient() {
        return clientSupplier.get();
    }

    /**
     * Add a comment to a Jira issue (jira_add_comment).
     * 
     * Python 스키마:
     * - issue_key (string, required): Jira issue key
     * - comment (string, required): Comment text in Markdown
     */
    public Mono<Map<String, Object>> addComment(Map<String, Object> params) {
        String issueKey = (String) params.get("issue_key");
        String comment = (String) params.get("comment");
        
        if (issueKey == null || issueKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("issue_key is required"));
        }
        if (comment == null || comment.isBlank()) {
            return Mono.error(new IllegalArgumentException("comment is required"));
        }
        
        return getClient().addComment(issueKey, comment)
                .map(node -> Map.<String, Object>of(
                        "success", true,
                        "id", node.path("id").asText()
                ))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Add a worklog entry to a Jira issue (jira_add_worklog).
     * 
     * Python 스키마:
     * - issue_key (string, required): Jira issue key
     * - time_spent (string, required): Time spent in Jira format (e.g., '1h 30m', '1d', '30m')
     * - comment (string, optional): Comment for the worklog in Markdown
     * - started (string, optional): Start time in ISO format
     * - original_estimate (string, optional): New original estimate
     * - remaining_estimate (string, optional): New remaining estimate
     */
    public Mono<Map<String, Object>> addWorklog(Map<String, Object> params) {
        String issueKey = (String) params.get("issue_key");
        String timeSpent = (String) params.get("time_spent");
        String comment = (String) params.get("comment");
        String started = (String) params.get("started");
        String originalEstimate = (String) params.get("original_estimate");
        String remainingEstimate = (String) params.get("remaining_estimate");
        
        if (issueKey == null || issueKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("issue_key is required"));
        }
        if (timeSpent == null || timeSpent.isBlank()) {
            return Mono.error(new IllegalArgumentException("time_spent is required"));
        }
        
        Map<String, Object> worklogData = new HashMap<>();
        worklogData.put("timeSpent", timeSpent);
        if (comment != null && !comment.isBlank()) worklogData.put("comment", comment);
        if (started != null && !started.isBlank()) worklogData.put("started", started);
        if (originalEstimate != null && !originalEstimate.isBlank()) worklogData.put("originalEstimate", originalEstimate);
        if (remainingEstimate != null && !remainingEstimate.isBlank()) worklogData.put("remainingEstimate", remainingEstimate);
        
        return getClient().addWorklog(issueKey, worklogData)
                .map(node -> Map.<String, Object>of(
                        "success", true,
                        "id", node.path("id").asText()
                ))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Create multiple Jira issues in a batch (jira_batch_create_issues).
     * 
     * Python 스키마:
     * - issues (string, required): JSON array of issue objects. Each object should contain:
     *   - project_key (required): The project key (e.g., 'PROJ')
     *   - summary (required): Issue summary/title
     *   - issue_type (required): Type of issue (e.g., 'Task', 'Bug')
     *   - description (optional): Issue description
     *   - assignee (optional): Assignee username or email
     *   - components (optional): Array of component names
     *   Example: [{"project_key": "PROJ", "summary": "Issue 1", "issue_type": "Task"}, {"project_key": "PROJ", "summary": "Issue 2", "issue_type": "Bug", "components": ["Frontend"]}]
     * - validate_only (boolean, default: false): If true, only validates the issues without creating them
     */
    public Mono<Map<String, Object>> batchCreateIssues(Map<String, Object> params) {
        String issuesJson = (String) params.get("issues");
        
        if (issuesJson == null || issuesJson.isBlank()) {
            return Mono.error(new IllegalArgumentException("issues JSON string is required"));
        }
        
        // Parse JSON string to List
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> issues = mapper.readValue(issuesJson, java.util.List.class);
            
            if (issues.isEmpty()) {
                return Mono.error(new IllegalArgumentException("issues array cannot be empty"));
            }
            
            return getClient().batchCreateIssues(issues)
                    .map(node -> {
                        var createdIssues = new java.util.ArrayList<Map<String, Object>>();
                        node.path("issues").forEach(issue -> createdIssues.add(Map.of(
                                "key", issue.path("key").asText(),
                                "id", issue.path("id").asText()
                        )));
                        return Map.<String, Object>of(
                                "success", true,
                                "issues", createdIssues
                        );
                    })
                    .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
        } catch (Exception e) {
            return Mono.just(Map.of("success", false, "error", "Invalid JSON format for issues: " + e.getMessage()));
        }
    }

    /**
     * Batch create multiple versions in a Jira project (jira_batch_create_versions).
     * 
     * Python 스키마:
     * - project_key (string, required): Jira project key (e.g., 'PROJ')
     * - versions (string, required): JSON array of version objects. Each object should contain:
     *   - name (required): Name of the version
     *   - startDate (optional): Start date (YYYY-MM-DD)
     *   - releaseDate (optional): Release date (YYYY-MM-DD)
     *   - description (optional): Description of the version
     *   Example: [{"name": "v1.0", "startDate": "2025-01-01", "releaseDate": "2025-02-01", "description": "First release"}, {"name": "v2.0"}]
     */
    public Mono<Map<String, Object>> batchCreateVersions(Map<String, Object> params) {
        String projectKey = (String) params.get("project_key");
        String versionsJson = (String) params.get("versions");
        
        if (projectKey == null || versionsJson == null || versionsJson.isBlank()) {
            return Mono.error(new IllegalArgumentException("project_key and versions are required"));
        }
        
        // Parse JSON string to List
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> versions = mapper.readValue(versionsJson, java.util.List.class);
            
            if (versions.isEmpty()) {
                return Mono.error(new IllegalArgumentException("versions array cannot be empty"));
            }
            
            return reactor.core.publisher.Flux.fromIterable(versions)
                    .flatMap(version -> {
                        Map<String, Object> versionParams = new HashMap<>(version);
                        versionParams.put("project_key", projectKey);
                        return createVersion(versionParams);
                    })
                    .collectList()
                    .map(results -> Map.<String, Object>of(
                            "success", true,
                            "versions", results
                    ))
                    .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
        } catch (Exception e) {
            return Mono.just(Map.of("success", false, "error", "Invalid JSON format for versions: " + e.getMessage()));
        }
    }

    /**
     * Create a new Jira issue (jira_create_issue).
     * 
     * Python 스키마:
     * - project_key (string, required): Project key
     * - summary (string, required): Issue summary/title
     * - issue_type (string, required): Issue type (Task, Bug, Story, Epic, Subtask)
     * - assignee (string, optional): Assignee's user identifier
     * - description (string, optional): Issue description
     * - components (string, optional): Comma-separated component names
     * - additional_fields (object, optional): Additional fields dictionary
     */
    public Mono<Map<String, Object>> createIssue(Map<String, Object> params) {
        String projectKey = (String) params.get("project_key");
        String summary = (String) params.get("summary");
        String issueType = (String) params.get("issue_type");
        String assignee = (String) params.get("assignee");
        String description = (String) params.get("description");
        String components = (String) params.get("components");
        @SuppressWarnings("unchecked")
        Map<String, Object> additionalFields = (Map<String, Object>) params.get("additional_fields");
        
        if (projectKey == null || summary == null || issueType == null) {
            return Mono.error(new IllegalArgumentException("project_key, summary, and issue_type are required"));
        }
        
        // Build fields map
        Map<String, Object> fields = new java.util.HashMap<>();
        fields.put("project", Map.of("key", projectKey));
        fields.put("summary", summary);
        fields.put("issuetype", Map.of("name", issueType));
        
        if (description != null) {
            fields.put("description", description);
        }
        if (assignee != null) {
            fields.put("assignee", Map.of("name", assignee));
        }
        if (components != null && !components.isBlank()) {
            String[] compArray = components.split(",");
            java.util.List<Map<String, String>> compList = new java.util.ArrayList<>();
            for (String comp : compArray) {
                compList.add(Map.of("name", comp.trim()));
            }
            fields.put("components", compList);
        }
        if (additionalFields != null) {
            fields.putAll(additionalFields);
        }
        
        return getClient().createIssue(Map.of("fields", fields))
                .map(node -> Map.<String, Object>of(
                        "success", true,
                        "key", node.path("key").asText(),
                        "id", node.path("id").asText()
                ))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Create a link between two Jira issues (jira_create_issue_link).
     * 
     * Python 스키마:
     * - link_type (string, required): Link type name (e.g., 'Blocks', 'Relates')
     * - inward_issue_key (string, required): Inward issue key
     * - outward_issue_key (string, required): Outward issue key
     * - comment (string, optional): Comment to add to the link
     * - comment_visibility (object, optional): Visibility settings for the comment
     */
    public Mono<Map<String, Object>> createIssueLink(Map<String, Object> params) {
        String linkType = (String) params.get("link_type");
        String inwardIssue = (String) params.get("inward_issue_key");
        String outwardIssue = (String) params.get("outward_issue_key");
        
        if (linkType == null || inwardIssue == null || outwardIssue == null) {
            return Mono.error(new IllegalArgumentException("link_type, inward_issue_key, and outward_issue_key are required"));
        }
        
        Map<String, Object> linkData = Map.of(
                "type", Map.of("name", linkType),
                "inwardIssue", Map.of("key", inwardIssue),
                "outwardIssue", Map.of("key", outwardIssue)
        );
        
        return getClient().createIssueLink(linkData)
                .then(Mono.just(Map.<String, Object>of("success", true, "message", "Link created")))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Create a remote issue link (jira_create_remote_issue_link).
     * 
     * Python 스키마:
     * - issue_key (string, required): Jira issue key
     * - url (string, required): URL to link to
     * - title (string, required): Title/name of the link
     * - summary (string, optional): Description of the link
     * - relationship (string, optional): Relationship description
     * - icon_url (string, optional): URL to 16x16 icon
     */
    public Mono<Map<String, Object>> createRemoteIssueLink(Map<String, Object> params) {
        String issueKey = (String) params.get("issue_key");
        String url = (String) params.get("url");
        String title = (String) params.get("title");
        
        if (issueKey == null || url == null || title == null) {
            return Mono.error(new IllegalArgumentException("issue_key, url, and title are required"));
        }
        
        Map<String, Object> linkData = Map.of(
                "object", Map.of("url", url, "title", title)
        );
        
        return getClient().createRemoteIssueLink(issueKey, linkData)
                .map(node -> Map.<String, Object>of(
                        "success", true,
                        "id", node.path("id").asText()
                ))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Create Jira sprint for a board (jira_create_sprint).
     * 
     * Python 스키마:
     * - board_id (string, required): Board ID
     * - sprint_name (string, required): Sprint name
     * - start_date (string, required): Start date (ISO 8601 format)
     * - end_date (string, required): End date (ISO 8601 format)
     * - goal (string, optional): Sprint goal
     */
    public Mono<Map<String, Object>> createSprint(Map<String, Object> params) {
        String boardId = (String) params.get("board_id");
        String name = (String) params.get("sprint_name");
        String startDate = (String) params.get("start_date");
        String endDate = (String) params.get("end_date");
        String goal = (String) params.get("goal");
        
        if (boardId == null || name == null) {
            return Mono.error(new IllegalArgumentException("board_id and sprint_name are required"));
        }
        
        Map<String, Object> sprintData = new HashMap<>();
        sprintData.put("name", name);
        sprintData.put("originBoardId", Integer.parseInt(boardId));
        if (startDate != null) sprintData.put("startDate", startDate);
        if (endDate != null) sprintData.put("endDate", endDate);
        if (goal != null) sprintData.put("goal", goal);
        
        return getClient().createSprint(boardId, sprintData)
                .map(node -> Map.<String, Object>of(
                        "success", true,
                        "id", node.path("id").asText(),
                        "name", node.path("name").asText()
                ))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Create a new fix version in a Jira project (jira_create_version).
     * 
     * Python 스키마:
     * - project_key (string, required): Project key
     * - name (string, required): Version name
     * - start_date (string, optional): Start date (YYYY-MM-DD)
     * - release_date (string, optional): Release date (YYYY-MM-DD)
     * - description (string, optional): Version description
     */
    public Mono<Map<String, Object>> createVersion(Map<String, Object> params) {
        String projectKey = (String) params.get("project_key");
        String name = (String) params.get("name");
        String description = (String) params.get("description");
        String startDate = (String) params.get("start_date");
        String releaseDate = (String) params.get("release_date");
        
        if (projectKey == null || name == null) {
            return Mono.error(new IllegalArgumentException("project_key and name are required"));
        }
        
        Map<String, Object> versionData = new HashMap<>();
        versionData.put("name", name);
        if (description != null) versionData.put("description", description);
        if (startDate != null) versionData.put("startDate", startDate);
        if (releaseDate != null) versionData.put("releaseDate", releaseDate);
        
        return getClient().createVersion(projectKey, versionData)
                .map(node -> Map.<String, Object>of(
                        "success", true,
                        "id", node.path("id").asText(),
                        "name", node.path("name").asText()
                ))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Delete an existing Jira issue (jira_delete_issue).
     * 
     * Python 스키마:
     * - issue_key (string, required): Jira issue key
     */
    public Mono<Map<String, Object>> deleteIssue(Map<String, Object> params) {
        String issueKey = (String) params.get("issue_key");
        if (issueKey == null || issueKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("issue_key is required"));
        }
        return getClient().deleteIssue(issueKey)
                .then(Mono.just(Map.<String, Object>of(
                        "success", true,
                        "message", "Issue " + issueKey + " deleted successfully"
                )))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Link an existing issue to an epic (jira_link_to_epic).
     * 
     * Python 스키마:
     * - issue_key (string, required): Issue key to link
     * - epic_key (string, required): Epic key to link to
     */
    public Mono<Map<String, Object>> linkToEpic(Map<String, Object> params) {
        String issueKey = (String) params.get("issue_key");
        String epicKey = (String) params.get("epic_key");
        
        if (issueKey == null || epicKey == null) {
            return Mono.error(new IllegalArgumentException("issue_key and epic_key are required"));
        }
        
        return getClient().linkToEpic(issueKey, epicKey)
                .map(this::convertToSimplified)
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Remove a link between two Jira issues (jira_remove_issue_link).
     * 
     * Python 스키마:
     * - link_id (string, required): ID of the link to remove
     */
    public Mono<Map<String, Object>> removeIssueLink(Map<String, Object> params) {
        String linkId = (String) params.get("link_id");
        if (linkId == null || linkId.isBlank()) {
            return Mono.error(new IllegalArgumentException("link_id is required"));
        }
        return getClient().removeIssueLink(linkId)
                .then(Mono.just(Map.<String, Object>of(
                        "success", true,
                        "message", "Link removed successfully"
                )))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Transition a Jira issue to a new status (jira_transition_issue).
     * 
     * Python 스키마:
     * - issue_key (string, required): Jira issue key
     * - transition_id (string, required): ID of the transition
     * - fields (object, optional): Fields to update during transition
     * - comment (string, optional): Comment for the transition
     */
    public Mono<Map<String, Object>> transitionIssue(Map<String, Object> params) {
        String issueKey = (String) params.get("issue_key");
        String transitionId = (String) params.get("transition_id");
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) params.get("fields");
        String comment = (String) params.get("comment");
        
        if (issueKey == null || issueKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("issue_key is required"));
        }
        if (transitionId == null || transitionId.isBlank()) {
            return Mono.error(new IllegalArgumentException("transition_id is required"));
        }
        
        Map<String, Object> transitionData = new HashMap<>();
        transitionData.put("transition", Map.of("id", transitionId));
        
        // Add optional fields
        if (fields != null && !fields.isEmpty()) {
            transitionData.put("fields", fields);
        }
        
        // Add optional comment
        if (comment != null && !comment.isBlank()) {
            transitionData.put("update", Map.of(
                "comment", java.util.List.of(Map.of(
                    "add", Map.of("body", comment)
                ))
            ));
        }
        
        return getClient().transitionIssue(issueKey, transitionData)
                .map(this::convertToSimplified)
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Update an existing Jira issue (jira_update_issue).
     * 
     * Python 스키마:
     * - issue_key (string, required): Jira issue key
     * - fields (object, required): Dictionary of fields to update
     * - additional_fields (object, optional): Additional fields dictionary
     * - attachments (string, optional): JSON array or comma-separated file paths
     */
    public Mono<Map<String, Object>> updateIssue(Map<String, Object> params) {
        String issueKey = (String) params.get("issue_key");
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) params.get("fields");
        
        if (issueKey == null || issueKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("issue_key is required"));
        }
        if (fields == null) {
            return Mono.error(new IllegalArgumentException("fields is required"));
        }
        
        return getClient().updateIssue(issueKey, fields)
                .map(this::convertToSimplified)
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Update jira sprint (jira_update_sprint).
     * 
     * Python 스키마:
     * - sprint_id (string, required): Sprint ID
     * - sprint_name (string, optional): New sprint name
     * - state (string, optional): New state (future|active|closed)
     * - start_date (string, optional): New start date
     * - end_date (string, optional): New end date
     * - goal (string, optional): New goal
     */
    public Mono<Map<String, Object>> updateSprint(Map<String, Object> params) {
        String sprintId = (String) params.get("sprint_id");
        String name = (String) params.get("sprint_name");
        String state = (String) params.get("state");
        String startDate = (String) params.get("start_date");
        String endDate = (String) params.get("end_date");
        String goal = (String) params.get("goal");
        
        if (sprintId == null || sprintId.isBlank()) {
            return Mono.error(new IllegalArgumentException("sprint_id is required"));
        }
        
        Map<String, Object> sprintData = new HashMap<>();
        if (name != null) sprintData.put("name", name);
        if (state != null) sprintData.put("state", state);
        if (startDate != null) sprintData.put("startDate", startDate);
        if (endDate != null) sprintData.put("endDate", endDate);
        if (goal != null) sprintData.put("goal", goal);
        
        return getClient().updateSprint(sprintId, sprintData)
                .map(node -> Map.<String, Object>of(
                        "success", true,
                        "id", node.path("id").asText(),
                        "name", node.path("name").asText(),
                        "state", node.path("state").asText()
                ))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    // Helper method
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
}
