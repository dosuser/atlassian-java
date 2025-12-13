package com.atlassian.mcp.confluence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Confluence REST API 클라이언트.
 * Confluence Cloud/Server/DC와 통신하는 기본 HTTP 클라이언트.
 * Python의 ConfluenceFetcher와 동일하게 요청별로 생성됨.
 */
public class ConfluenceClient {
    private final WebClient webClient;
    private final ObjectMapper mapper;

    /**
     * ConfluenceClient 생성자.
     * Python의 ConfluenceFetcher(config)와 동일한 패턴.
     */
    public ConfluenceClient(String baseUrl, String token, ObjectMapper mapper) {
        this.mapper = mapper;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * 페이지 조회 (ID 기반).
     * 
     * @param pageId 페이지 ID
     * @param expand 확장할 필드 (예: body.storage, version)
     * @return 페이지 JSON 데이터
     */
    public Mono<JsonNode> getPage(String pageId, String expand) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/rest/api/content/{pageId}");
                    if (expand != null && !expand.isBlank()) {
                        builder.queryParam("expand", expand);
                    }
                    return builder.build(pageId);
                })
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    /**
     * 페이지 검색 (제목 기반).
     * 
     * @param spaceKey 스페이스 키
     * @param title 페이지 제목
     * @param expand 확장할 필드
     * @return 페이지 JSON 데이터
     */
    public Mono<JsonNode> getPageByTitle(String spaceKey, String title, String expand) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/rest/api/content")
                            .queryParam("type", "page")
                            .queryParam("spaceKey", spaceKey)
                            .queryParam("title", title);
                    if (expand != null && !expand.isBlank()) {
                        builder.queryParam("expand", expand);
                    }
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> response.path("results").get(0));
    }

    /**
     * CQL 검색.
     * 
     * @param cql CQL 쿼리 문자열
     * @param limit 최대 결과 개수
     * @return 검색 결과 JSON
     */
    public Mono<JsonNode> search(String cql, int limit) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/rest/api/content/search")
                        .queryParam("cql", cql)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> getPageChildren(String pageId, int start, int limit, String expand) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/rest/api/content/{pageId}/child/page")
                            .queryParam("start", start)
                            .queryParam("limit", limit);
                    if (expand != null) builder.queryParam("expand", expand);
                    return builder.build(pageId);
                })
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> getPageComments(String pageId) {
        return webClient.get()
                .uri("/rest/api/content/{pageId}/child/comment?expand=body.storage", pageId)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> getPageLabels(String pageId) {
        return webClient.get()
                .uri("/rest/api/content/{pageId}/label", pageId)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> addPageLabel(String pageId, String labelName) {
        return webClient.post()
                .uri("/rest/api/content/{pageId}/label", pageId)
                .bodyValue(new Object[]{Map.of("name", labelName)})
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> createPage(Map<String, Object> pageData) {
        return webClient.post()
                .uri("/rest/api/content")
                .bodyValue(pageData)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> updatePage(String pageId, Map<String, Object> pageData) {
        return webClient.put()
                .uri("/rest/api/content/{pageId}", pageId)
                .bodyValue(pageData)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<Boolean> deletePage(String pageId) {
        return webClient.delete()
                .uri("/rest/api/content/{pageId}", pageId)
                .retrieve()
                .bodyToMono(Void.class)
                .thenReturn(true)
                .onErrorReturn(false);
    }

    public Mono<JsonNode> addComment(String pageId, String content) {
        Map<String, Object> commentData = Map.of(
                "type", "comment",
                "container", Map.of("id", pageId, "type", "page"),
                "body", Map.of("storage", Map.of("value", content, "representation", "storage"))
        );
        return webClient.post()
                .uri("/rest/api/content")
                .bodyValue(commentData)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> searchUser(String cql, int limit) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/rest/api/search/user")
                        .queryParam("cql", cql)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class);
    }
}
