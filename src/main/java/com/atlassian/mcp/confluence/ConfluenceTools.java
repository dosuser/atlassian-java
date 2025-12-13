 package com.atlassian.mcp.confluence;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Confluence MCP 도구 구현.
 * Python mcp-atlassian의 Confluence 도구를 Java로 포팅.
 * Python의 get_confluence_fetcher()처럼 요청별로 Client를 동적 생성.
 */
public class ConfluenceTools {
    private final Supplier<ConfluenceClient> clientSupplier;
    
    public ConfluenceTools(Supplier<ConfluenceClient> clientSupplier) {
        this.clientSupplier = clientSupplier;
    }
    
    /**
     * 현재 요청의 ConfluenceClient 가져오기.
     */
    private ConfluenceClient getClient() {
        return clientSupplier.get();
    }

    /**
     * Confluence 페이지 조회 (confluence_get_page).
     * 
     * Python 스키마:
     * - page_id (string, optional): Confluence page ID (numeric)
     * - title (string, optional): Page title (use with space_key)
     * - space_key (string, optional): Space key (use with title)
     * - include_metadata (boolean, default: true): Include page metadata
     * - convert_to_markdown (boolean, default: true): Convert to markdown
     * 
     * @param params 파라미터 맵
     * @return 페이지 정보 JSON
     */
    public Mono<Map<String, Object>> getPage(Map<String, Object> params) {
        String pageId = (String) params.get("page_id");
        String title = (String) params.get("title");
        String spaceKey = (String) params.get("space_key");
        boolean includeMetadata = (boolean) params.getOrDefault("include_metadata", true);
        String expand = "body.storage,version,space";

        Mono<JsonNode> pageMono;
        if (pageId != null && !pageId.isBlank()) {
            pageMono = getClient().getPage(pageId, expand);
        } else if (title != null && !title.isBlank() && spaceKey != null && !spaceKey.isBlank()) {
            pageMono = getClient().getPageByTitle(spaceKey, title, expand);
        } else {
            return Mono.error(new IllegalArgumentException(
                    "Either 'page_id' OR both 'title' and 'space_key' must be provided"));
        }

        return pageMono
                .map(node -> convertPageToSimplified(node, includeMetadata))
                .onErrorResume(e -> Mono.just(Map.of(
                        "success", false,
                        "error", e.getMessage()
                )));
    }

    /**
     * CQL 검색 (confluence_search).
     * 
     * Python 스키마:
     * - query (string, required): Search query - can be simple text or CQL query string
     * - limit (integer, default: 10): Maximum number of results (1-50)
     * - spaces_filter (string, optional): Comma-separated space keys to filter results
     * 
     * @param params 파라미터 맵
     * @return 검색 결과 JSON
     */
    public Mono<Map<String, Object>> search(Map<String, Object> params) {
        // cql 또는 query 파라미터를 허용
        String queryParam = (String) params.get("cql");
        if (queryParam == null || queryParam.isBlank()) {
            queryParam = (String) params.get("query");
        }
        final String query = queryParam;  // final로 선언
        int limit = (int) params.getOrDefault("limit", 10);

        if (query == null || query.isBlank()) {
            return Mono.error(new IllegalArgumentException("cql or query is required"));
        }

        // 단순 검색어를 CQL로 변환
        String cql = query;
        if (!query.contains("=") && !query.contains("~") && !query.contains(" AND ") && !query.contains(" OR ")) {
            cql = "text ~ \"" + query + "\"";
        }
        
        // CQL에서 space key에 특수문자(~)가 있는 경우 자동으로 quote 처리
        cql = CqlUtils.autoQuoteSpaceKeys(cql);

        return getClient().search(cql, limit)
                .map(this::convertSearchResults)
                .onErrorResume(e -> Mono.just(Map.of(
                        "success", false,
                        "error", e.getMessage(),
                        "query", query
                )));
    }

    /**
     * JsonNode를 단순화된 페이지 Map으로 변환.
     */
    private Map<String, Object> convertPageToSimplified(JsonNode node, boolean includeMetadata) {
        Map<String, Object> result = new HashMap<>();
        
        if (includeMetadata) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("id", node.path("id").asText());
            metadata.put("title", node.path("title").asText());
            metadata.put("type", node.path("type").asText());
            metadata.put("space", node.path("space").path("key").asText());
            
            JsonNode version = node.path("version");
            metadata.put("version", version.path("number").asInt());
            metadata.put("updated", version.path("when").asText());
            metadata.put("updatedBy", version.path("by").path("displayName").asText());
            
            result.put("metadata", metadata);
        }
        
        // 컨텐츠 추출 (HTML 형식)
        String content = node.path("body").path("storage").path("value").asText();
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("value", content);
        result.put("content", contentMap);
        
        result.put("success", true);
        return result;
    }

    /**
     * 검색 결과를 단순화된 형식으로 변환.
     */
    private Map<String, Object> convertSearchResults(JsonNode node) {
        Map<String, Object> result = new HashMap<>();
        result.put("total", node.path("totalSize").asInt());
        
        var pages = new java.util.ArrayList<Map<String, Object>>();
        node.path("results").forEach(page -> {
            Map<String, Object> pageMap = new HashMap<>();
            pageMap.put("id", page.path("id").asText());
            pageMap.put("title", page.path("title").asText());
            pageMap.put("type", page.path("type").asText());
            pageMap.put("space", page.path("space").path("key").asText());
            pages.add(pageMap);
        });
        
        result.put("results", pages);
        result.put("success", true);
        return result;
    }

    /**
     * Confluence 페이지 하위 페이지 조회 (confluence_get_page_children).
     * 
     * Python 스키마:
     * - parent_id (string, required): Parent page ID
     * - expand (string, default: "version"): Fields to expand
     * - limit (integer, default: 25): Maximum number of results (1-50)
     * - include_content (boolean, default: false): Include page content
     * - convert_to_markdown (boolean, default: true): Convert to markdown
     * - start (integer, default: 0): Starting index for pagination
     * 
     * @param params 파라미터 맵
     * @return 하위 페이지 목록 JSON
     */
    public Mono<Map<String, Object>> getPageChildren(Map<String, Object> params) {
        String pageId = (String) params.get("parent_id");
        int start = (int) params.getOrDefault("start", 0);
        int limit = (int) params.getOrDefault("limit", 25);
        String expand = (String) params.getOrDefault("expand", "version");
        // TODO: Python 파라미터 구현 필요 - include_content, convert_to_markdown
        // boolean includeContent = (boolean) params.getOrDefault("include_content", false);
        // boolean convertToMarkdown = (boolean) params.getOrDefault("convert_to_markdown", true);
        
        if (pageId == null || pageId.isBlank()) {
            return Mono.error(new IllegalArgumentException("parent_id is required"));
        }
        
        return getClient().getPageChildren(pageId, start, limit, expand)
                .map(node -> {
                    var children = new java.util.ArrayList<Map<String, Object>>();
                    node.path("results").forEach(page -> {
                        Map<String, Object> pageMap = new HashMap<>();
                        pageMap.put("id", page.path("id").asText());
                        pageMap.put("title", page.path("title").asText());
                        pageMap.put("type", page.path("type").asText());
                        children.add(pageMap);
                    });
                    return Map.<String, Object>of(
                            "success", true,
                            "parent_id", pageId,
                            "count", children.size(),
                            "results", children
                    );
                })
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Confluence 페이지 댓글 조회 (confluence_get_comments).
     * 
     * Python 스키마:
     * - page_id (string, required): Page ID
     * 
     * @param params 파라미터 맵
     * @return 댓글 목록 JSON
     */
    public Mono<Map<String, Object>> getComments(Map<String, Object> params) {
        String pageId = (String) params.get("page_id");
        if (pageId == null || pageId.isBlank()) {
            return Mono.error(new IllegalArgumentException("page_id is required"));
        }
        
        return getClient().getPageComments(pageId)
                .map(node -> {
                    var comments = new java.util.ArrayList<Map<String, Object>>();
                    node.path("results").forEach(comment -> {
                        Map<String, Object> commentMap = new HashMap<>();
                        commentMap.put("id", comment.path("id").asText());
                        commentMap.put("content", comment.path("body").path("storage").path("value").asText());
                        commentMap.put("author", comment.path("history").path("createdBy").path("displayName").asText());
                        comments.add(commentMap);
                    });
                    return Map.<String, Object>of("success", true, "comments", comments);
                })
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Confluence 페이지 라벨 조회 (confluence_get_labels).
     * 
     * Python 스키마:
     * - page_id (string, required): Page ID
     * 
     * @param params 파라미터 맵
     * @return 라벨 목록 JSON
     */
    public Mono<Map<String, Object>> getLabels(Map<String, Object> params) {
        String pageId = (String) params.get("page_id");
        if (pageId == null || pageId.isBlank()) {
            return Mono.error(new IllegalArgumentException("page_id is required"));
        }
        
        return getClient().getPageLabels(pageId)
                .map(node -> {
                    var labels = new java.util.ArrayList<String>();
                    node.path("results").forEach(label -> labels.add(label.path("name").asText()));
                    return Map.<String, Object>of("success", true, "labels", labels);
                })
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Confluence 페이지 라벨 추가 (confluence_add_label).
     * 
     * Python 스키마:
     * - page_id (string, required): Page ID
     * - name (string, required): Label name
     * 
     * @param params 파라미터 맵
     * @return 업데이트된 라벨 목록 JSON
     */
    public Mono<Map<String, Object>> addLabel(Map<String, Object> params) {
        String pageId = (String) params.get("page_id");
        String name = (String) params.get("name");
        
        if (pageId == null || name == null) {
            return Mono.error(new IllegalArgumentException("page_id and name are required"));
        }
        
        return getClient().addPageLabel(pageId, name)
                .map(node -> {
                    var labels = new java.util.ArrayList<String>();
                    node.path("results").forEach(label -> labels.add(label.path("name").asText()));
                    return Map.<String, Object>of("success", true, "labels", labels);
                })
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Confluence 페이지 생성 (confluence_create_page).
     * 
     * Python 스키마:
     * - space_key (string, required): Space key
     * - title (string, required): Page title
     * - content (string, required): Page content
     * - parent_id (string, optional): Parent page ID
     * - content_format (string, default: "markdown"): Format (markdown/wiki/storage)
     * - enable_heading_anchors (boolean, default: false): Enable heading anchors
     * 
     * @param params 파라미터 맵
     * @return 생성된 페이지 정보 JSON
     */
    public Mono<Map<String, Object>> createPage(Map<String, Object> params) {
        String spaceKey = (String) params.get("space_key");
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        String parentId = (String) params.get("parent_id");
        // TODO: Python 파라미터 구현 필요 - content_format (markdown/wiki/storage 변환), enable_heading_anchors
        // String contentFormat = (String) params.getOrDefault("content_format", "markdown");
        // boolean enableHeadingAnchors = (boolean) params.getOrDefault("enable_heading_anchors", false);
        
        if (spaceKey == null || title == null || content == null) {
            return Mono.error(new IllegalArgumentException("space_key, title, and content are required"));
        }
        
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("type", "page");
        pageData.put("title", title);
        pageData.put("space", Map.of("key", spaceKey));
        pageData.put("body", Map.of("storage", Map.of("value", content, "representation", "storage")));
        if (parentId != null) {
            pageData.put("ancestors", new Object[]{Map.of("id", parentId)});
        }
        
        return getClient().createPage(pageData)
                .map(node -> Map.<String, Object>of(
                        "success", true,
                        "id", node.path("id").asText(),
                        "title", node.path("title").asText(),
                        "url", node.path("_links").path("webui").asText()
                ))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Confluence 페이지 업데이트 (confluence_update_page).
     * 
     * Python 스키마:
     * - page_id (string, required): Page ID
     * - title (string, required): Page title
     * - content (string, required): Page content
     * - is_minor_edit (boolean, default: false): Minor edit flag
     * - version_comment (string, optional): Version comment
     * - parent_id (string, optional): New parent page ID
     * - content_format (string, default: "markdown"): Format (markdown/wiki/storage)
     * - enable_heading_anchors (boolean, default: false): Enable heading anchors
     * 
     * @param params 파라미터 맵
     * @return 업데이트된 페이지 정보 JSON
     */
    public Mono<Map<String, Object>> updatePage(Map<String, Object> params) {
        String pageId = (String) params.get("page_id");
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        // TODO: Python 파라미터 구현 필요 - is_minor_edit, version_comment, parent_id, content_format, enable_heading_anchors
        // boolean isMinorEdit = (boolean) params.getOrDefault("is_minor_edit", false);
        // String versionComment = (String) params.get("version_comment");
        // String parentId = (String) params.get("parent_id");
        // String contentFormat = (String) params.getOrDefault("content_format", "markdown");
        // boolean enableHeadingAnchors = (boolean) params.getOrDefault("enable_heading_anchors", false);
        
        if (pageId == null || title == null || content == null) {
            return Mono.error(new IllegalArgumentException("page_id, title, and content are required"));
        }
        
        // Client를 먼저 가져와서 flatMap 안에서 재사용 (RequestContext 유지)
        ConfluenceClient client = getClient();
        return client.getPage(pageId, "version")
                .flatMap(currentPage -> {
                    int currentVersion = currentPage.path("version").path("number").asInt();
                    Map<String, Object> pageData = new HashMap<>();
                    pageData.put("type", "page");
                    pageData.put("title", title);
                    pageData.put("version", Map.of("number", currentVersion + 1));
                    pageData.put("body", Map.of("storage", Map.of("value", content, "representation", "storage")));
                    return client.updatePage(pageId, pageData);
                })
                .map(node -> Map.<String, Object>of(
                        "success", true,
                        "id", node.path("id").asText(),
                        "title", node.path("title").asText(),
                        "version", node.path("version").path("number").asInt()
                ))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Confluence 페이지 삭제 (confluence_delete_page).
     * 
     * Python 스키마:
     * - page_id (string, required): Page ID to delete
     * 
     * @param params 파라미터 맵
     * @return 삭제 결과 JSON
     */
    public Mono<Map<String, Object>> deletePage(Map<String, Object> params) {
        String pageId = (String) params.get("page_id");
        if (pageId == null || pageId.isBlank()) {
            return Mono.error(new IllegalArgumentException("page_id is required"));
        }
        
        return getClient().deletePage(pageId)
                .map(success -> Map.<String, Object>of(
                        "success", success,
                        "message", success ? "Page deleted successfully" : "Failed to delete page"
                ))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Confluence 페이지 댓글 추가 (confluence_add_comment).
     * 
     * Python 스키마:
     * - page_id (string, required): Page ID
     * - content (string, required): Comment content in Markdown
     * 
     * @param params 파라미터 맵
     * @return 생성된 댓글 정보 JSON
     */
    public Mono<Map<String, Object>> addComment(Map<String, Object> params) {
        String pageId = (String) params.get("page_id");
        String content = (String) params.get("content");
        
        if (pageId == null || content == null) {
            return Mono.error(new IllegalArgumentException("page_id and content are required"));
        }
        
        return getClient().addComment(pageId, content)
                .map(node -> Map.<String, Object>of(
                        "success", true,
                        "id", node.path("id").asText()
                ))
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }

    /**
     * Confluence 사용자 검색 (confluence_search_user).
     * 
     * Python 스키마:
     * - query (string, required): Search query - CQL query string
     * - limit (integer, default: 10): Maximum number of results (1-50)
     * 
     * @param params 파라미터 맵
     * @return 사용자 검색 결과 JSON
     */
    public Mono<Map<String, Object>> searchUser(Map<String, Object> params) {
        String query = (String) params.get("query");
        int limit = (int) params.getOrDefault("limit", 10);
        
        if (query == null || query.isBlank()) {
            return Mono.error(new IllegalArgumentException("query is required"));
        }
        
        String cql = query;
        if (!query.contains("user.")) {
            cql = "user.fullname ~ \"" + query + "\"";
        }
        
        return getClient().searchUser(cql, limit)
                .map(node -> {
                    var users = new java.util.ArrayList<Map<String, Object>>();
                    node.path("results").forEach(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("accountId", user.path("accountId").asText());
                        userMap.put("displayName", user.path("displayName").asText());
                        userMap.put("email", user.path("email").asText());
                        users.add(userMap);
                    });
                    return Map.<String, Object>of("success", true, "users", users);
                })
                .onErrorResume(e -> Mono.just(Map.of("success", false, "error", e.getMessage())));
    }
}
