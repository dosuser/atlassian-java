package com.atlassian.mcp.jira;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Jira REST API 클라이언트.
 * Jira Cloud/Server/DC와 통신하는 기본 HTTP 클라이언트.
 * Python의 JiraFetcher와 동일하게 요청별로 생성됨.
 */
public class JiraClient {
    private final WebClient webClient;
    private final ObjectMapper mapper;

    /**
     * JiraClient 생성자.
     * Python의 JiraFetcher(config)와 동일한 패턴.
     */
    public JiraClient(String baseUrl, String token, ObjectMapper mapper) {
        this.mapper = mapper;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Jira 이슈 조회.
     * 
     * @param issueKey 이슈 키 (예: PROJ-123)
     * @param fields 반환할 필드 목록 (쉼표 구분)
     * @param expand 확장할 필드 (예: renderedFields, transitions, changelog)
     * @return 이슈 JSON 데이터
     */
    public Mono<JsonNode> getIssue(String issueKey, String fields, String expand) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/rest/api/2/issue/{issueKey}");
                    if (fields != null && !fields.isBlank()) {
                        builder.queryParam("fields", fields);
                    }
                    if (expand != null && !expand.isBlank()) {
                        builder.queryParam("expand", expand);
                    }
                    return builder.build(issueKey);
                })
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    /**
     * JQL 검색.
     * 
     * @param jql JQL 쿼리 문자열
     * @param fields 반환할 필드 목록
     * @param startAt 페이지네이션 시작 인덱스
     * @param maxResults 최대 결과 개수
     * @param expand 확장할 필드
     * @return 검색 결과 JSON
     */
    public Mono<JsonNode> searchIssues(String jql, String fields, int startAt, int maxResults, String expand) {
        var body = Map.of(
                "jql", jql,
                "startAt", startAt,
                "maxResults", maxResults,
                "fields", fields != null ? fields.split(",") : new String[]{"summary", "status"},
                "expand", expand != null ? expand.split(",") : new String[]{}
        );

        return webClient.post()
                .uri("/rest/api/2/search")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> getUserProfile(String userIdentifier) {
        return webClient.get()
                .uri("/rest/api/2/user?username={user}", userIdentifier)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(e -> 
                    webClient.get()
                        .uri("/rest/api/2/user?accountId={accountId}", userIdentifier)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                );
    }

    public Mono<JsonNode> getFields() {
        return webClient.get()
                .uri("/rest/api/2/field")
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> getTransitions(String issueKey) {
        return webClient.get()
                .uri("/rest/api/2/issue/{issueKey}/transitions", issueKey)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> getWorklogs(String issueKey) {
        return webClient.get()
                .uri("/rest/api/2/issue/{issueKey}/worklog", issueKey)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> getAgileBoards(String boardName, String projectKey, String boardType, int startAt, int maxResults) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/rest/agile/1.0/board")
                            .queryParam("startAt", startAt)
                            .queryParam("maxResults", maxResults);
                    if (boardName != null) builder.queryParam("name", boardName);
                    if (projectKey != null) builder.queryParam("projectKeyOrId", projectKey);
                    if (boardType != null) builder.queryParam("type", boardType);
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> getBoardIssues(String boardId, String jql, String fields, int startAt, int maxResults, String expand) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/rest/agile/1.0/board/{boardId}/issue")
                            .queryParam("startAt", startAt)
                            .queryParam("maxResults", maxResults);
                    if (jql != null) builder.queryParam("jql", jql);
                    if (fields != null) builder.queryParam("fields", fields);
                    if (expand != null) builder.queryParam("expand", expand);
                    return builder.build(boardId);
                })
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> getSprintsFromBoard(String boardId, String state, int startAt, int maxResults) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/rest/agile/1.0/board/{boardId}/sprint")
                            .queryParam("startAt", startAt)
                            .queryParam("maxResults", maxResults);
                    if (state != null) builder.queryParam("state", state);
                    return builder.build(boardId);
                })
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> getSprintIssues(String sprintId, String fields, int startAt, int maxResults) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/rest/agile/1.0/sprint/{sprintId}/issue")
                            .queryParam("startAt", startAt)
                            .queryParam("maxResults", maxResults);
                    if (fields != null) builder.queryParam("fields", fields);
                    return builder.build(sprintId);
                })
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> getIssueLinkTypes() {
        return webClient.get()
                .uri("/rest/api/2/issueLinkType")
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> getProjectVersions(String projectKey) {
        return webClient.get()
                .uri("/rest/api/2/project/{projectKey}/versions", projectKey)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> getAllProjects() {
        return webClient.get()
                .uri("/rest/api/2/project")
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> createIssue(Map<String, Object> issueData) {
        return webClient.post()
                .uri("/rest/api/2/issue")
                .bodyValue(Map.of("fields", issueData))
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> updateIssue(String issueKey, Map<String, Object> updateData) {
        return webClient.put()
                .uri("/rest/api/2/issue/{issueKey}", issueKey)
                .bodyValue(Map.of("fields", updateData))
                .retrieve()
                .toBodilessEntity()
                .then(getIssue(issueKey, null, null));
    }

    public Mono<Void> deleteIssue(String issueKey) {
        return webClient.delete()
                .uri("/rest/api/2/issue/{issueKey}", issueKey)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<JsonNode> addComment(String issueKey, String comment) {
        return webClient.post()
                .uri("/rest/api/2/issue/{issueKey}/comment", issueKey)
                .bodyValue(Map.of("body", comment))
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> addWorklog(String issueKey, Map<String, Object> worklogData) {
        return webClient.post()
                .uri("/rest/api/2/issue/{issueKey}/worklog", issueKey)
                .bodyValue(worklogData)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> transitionIssue(String issueKey, Map<String, Object> transitionData) {
        return webClient.post()
                .uri("/rest/api/2/issue/{issueKey}/transitions", issueKey)
                .bodyValue(transitionData)
                .retrieve()
                .toBodilessEntity()
                .then(getIssue(issueKey, null, null));
    }

    public Mono<JsonNode> createIssueLink(Map<String, Object> linkData) {
        return webClient.post()
                .uri("/rest/api/2/issueLink")
                .bodyValue(linkData)
                .retrieve()
                .toBodilessEntity()
                .thenReturn(mapper.createObjectNode().put("success", true));
    }

    public Mono<JsonNode> createRemoteIssueLink(String issueKey, Map<String, Object> linkData) {
        return webClient.post()
                .uri("/rest/api/2/issue/{issueKey}/remotelink", issueKey)
                .bodyValue(linkData)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> removeIssueLink(String linkId) {
        return webClient.delete()
                .uri("/rest/api/2/issueLink/{linkId}", linkId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorReturn(mapper.createObjectNode());
    }

    public Mono<JsonNode> linkToEpic(String issueKey, String epicKey) {
        return webClient.post()
                .uri("/rest/agile/1.0/epic/{epicKey}/issue", epicKey)
                .bodyValue(Map.of("issues", new String[]{issueKey}))
                .retrieve()
                .toBodilessEntity()
                .then(getIssue(issueKey, null, null));
    }

    public Mono<JsonNode> batchCreateIssues(java.util.List<Map<String, Object>> issuesData) {
        return webClient.post()
                .uri("/rest/api/2/issue/bulk")
                .bodyValue(Map.of("issueUpdates", issuesData.stream()
                        .map(data -> Map.of("fields", data))
                        .toList()))
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> createSprint(String boardId, Map<String, Object> sprintData) {
        return webClient.post()
                .uri("/rest/agile/1.0/sprint")
                .bodyValue(sprintData)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> updateSprint(String sprintId, Map<String, Object> sprintData) {
        return webClient.put()
                .uri("/rest/agile/1.0/sprint/{sprintId}", sprintId)
                .bodyValue(sprintData)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> createVersion(String projectKey, Map<String, Object> versionData) {
        Map<String, Object> data = new HashMap<>(versionData);
        data.put("project", projectKey);
        return webClient.post()
                .uri("/rest/api/2/version")
                .bodyValue(data)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> batchGetChangelogs(java.util.List<String> issueKeys) {
        return reactor.core.publisher.Flux.fromIterable(issueKeys)
                .flatMap(key -> getIssue(key, null, "changelog"))
                .collectList()
                .map(list -> {
                    com.fasterxml.jackson.databind.node.ArrayNode array = mapper.createArrayNode();
                    list.forEach(array::add);
                    return array;
                });
    }
}
