package com.atlassian.mcp.jira;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Jira MCP 도구 구현.
 * Python mcp-atlassian의 Jira 도구를 Java로 포팅.
 * Python의 get_jira_fetcher()처럼 요청별로 Client를 동적 생성.
 */
public class JiraTools {
    private final Supplier<JiraClient> clientSupplier;

    public JiraTools(Supplier<JiraClient> clientSupplier) {
        this.clientSupplier = clientSupplier;
    }
    
    /**
     * 현재 요청의 JiraClient 가져오기.
     */
    private JiraClient getClient() {
        return clientSupplier.get();
    }

    /**
     * Jira 이슈 조회 (jira_get_issue).
     * 
     * Python 원본: async def get_issue(ctx, issue_key, fields, expand, ...)
     * 
     * @param params 파라미터 맵 (issue_key, fields, expand, comment_limit 등)
     * @return 이슈 정보 JSON
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

    /**
     * JQL 검색 (jira_search).
     * 
     * Python 원본: async def search(ctx, jql, fields, limit, start_at, ...)
     * 
     * @param params 파라미터 맵 (jql, fields, limit, start_at 등)
     * @return 검색 결과 JSON
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
     * JsonNode를 단순화된 Map으로 변환.
     */
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

    /**
     * 검색 결과를 단순화된 형식으로 변환.
     */
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

    public Mono<Map<String, Object>> getProjectIssues(Map<String, Object> params) {
        String projectKey = (String) params.get("project_key");
        int limit = (int) params.getOrDefault("limit", 10);
        int startAt = (int) params.getOrDefault("start_at", 0);
        
        if (projectKey == null || projectKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("project_key is required"));
        }
        
        String jql = "project=" + projectKey;
        return getClient().searchIssues(jql, "summary,status,assignee", startAt, limit, null)
                .map(this::convertSearchResults);
    }

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

    public Mono<Map<String, Object>> getBoardIssues(Map<String, Object> params) {
        String boardId = (String) params.get("board_id");
        String jql = (String) params.get("jql");
        String fields = (String) params.getOrDefault("fields", "summary,status");
        int startAt = (int) params.getOrDefault("start_at", 0);
        int limit = (int) params.getOrDefault("limit", 10);
        String expand = (String) params.get("expand");
        
        if (boardId == null || boardId.isBlank()) {
            return Mono.error(new IllegalArgumentException("board_id is required"));
        }
        
        return getClient().getBoardIssues(boardId, jql, fields, startAt, limit, expand)
                .map(this::convertSearchResults);
    }

    public Mono<Map<String, Object>> getSprintsFromBoard(Map<String, Object> params) {
        String boardId = (String) params.get("board_id");
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

    public Mono<Map<String, Object>> getSprintIssues(Map<String, Object> params) {
        String sprintId = (String) params.get("sprint_id");
        String fields = (String) params.getOrDefault("fields", "summary,status");
        int startAt = (int) params.getOrDefault("start_at", 0);
        int limit = (int) params.getOrDefault("limit", 10);
        
        if (sprintId == null || sprintId.isBlank()) {
            return Mono.error(new IllegalArgumentException("sprint_id is required"));
        }
        
        return getClient().getSprintIssues(sprintId, fields, startAt, limit)
                .map(this::convertSearchResults);
    }

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

    public Mono<Map<String, Object>> getProjectVersions(Map<String, Object> params) {
        String projectKey = (String) params.get("project_key");
        if (projectKey == null || projectKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("project_key is required"));
        }
        return getClient().getProjectVersions(projectKey)
                .map(versions -> Map.<String, Object>of("success", true, "versions", versions))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

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

    public Mono<Map<String, Object>> createIssue(Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) params.get("fields");
        if (fields == null) {
            return Mono.error(new IllegalArgumentException("fields is required"));
        }
        return getClient().createIssue(fields)
                .map(node -> Map.<String, Object>of(
                        "success", true,
                        "key", node.path("key").asText(),
                        "id", node.path("id").asText()
                ))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

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

    public Mono<Map<String, Object>> addWorklog(Map<String, Object> params) {
        String issueKey = (String) params.get("issue_key");
        String timeSpent = (String) params.get("time_spent");
        String comment = (String) params.get("comment");
        
        if (issueKey == null || issueKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("issue_key is required"));
        }
        if (timeSpent == null || timeSpent.isBlank()) {
            return Mono.error(new IllegalArgumentException("time_spent is required"));
        }
        
        Map<String, Object> worklogData = new HashMap<>();
        worklogData.put("timeSpent", timeSpent);
        if (comment != null) worklogData.put("comment", comment);
        
        return getClient().addWorklog(issueKey, worklogData)
                .map(node -> Map.<String, Object>of(
                        "success", true,
                        "id", node.path("id").asText()
                ))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    public Mono<Map<String, Object>> transitionIssue(Map<String, Object> params) {
        String issueKey = (String) params.get("issue_key");
        String transitionId = (String) params.get("transition_id");
        
        if (issueKey == null || issueKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("issue_key is required"));
        }
        if (transitionId == null || transitionId.isBlank()) {
            return Mono.error(new IllegalArgumentException("transition_id is required"));
        }
        
        Map<String, Object> transitionData = Map.of(
                "transition", Map.of("id", transitionId)
        );
        
        return getClient().transitionIssue(issueKey, transitionData)
                .map(this::convertToSimplified)
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

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

    public Mono<Map<String, Object>> batchCreateIssues(Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> issues = (java.util.List<Map<String, Object>>) params.get("issues");
        
        if (issues == null || issues.isEmpty()) {
            return Mono.error(new IllegalArgumentException("issues list is required"));
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
    }

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

    public Mono<Map<String, Object>> batchCreateVersions(Map<String, Object> params) {
        String projectKey = (String) params.get("project_key");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> versions = (java.util.List<Map<String, Object>>) params.get("versions");
        
        if (projectKey == null || versions == null || versions.isEmpty()) {
            return Mono.error(new IllegalArgumentException("project_key and versions are required"));
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
    }

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

    public Mono<Map<String, Object>> downloadAttachments(Map<String, Object> params) {
        String issueKey = (String) params.get("issue_key");
        String targetDir = (String) params.get("target_dir");
        
        if (issueKey == null || targetDir == null) {
            return Mono.error(new IllegalArgumentException("issue_key and target_dir are required"));
        }
        
        return Mono.just(Map.<String, Object>of(
                "success", false,
                "error", "File download not implemented in this version. Use direct REST API calls."
        ));
    }
}
