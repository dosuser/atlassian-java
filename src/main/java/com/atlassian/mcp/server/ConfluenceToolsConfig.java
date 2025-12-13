package com.atlassian.mcp.server;

import com.atlassian.mcp.core.ToolRegistry;
import com.atlassian.mcp.confluence.ConfluenceTools;

import java.util.Map;

/**
 * Confluence 도구 등록 설정
 */
public class ConfluenceToolsConfig {
    
    public static void configure(ToolRegistry reg, ConfluenceTools confluenceTools) {
        reg.register(
            "confluence_search",
            "Search Confluence content using simple terms or CQL.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "query", Map.of("type", "string", "description", "Search query - can be simple text or CQL query string"),
                    "limit", Map.of("type", "integer", "description", "Maximum number of results (1-50)", "default", 10),
                    "spaces_filter", Map.of("type", "string", "description", "Comma-separated space keys to filter results")
                ),
                "required", java.util.List.of("query")
            ),
            params -> confluenceTools.search(asMap(params)).toFuture().join()
        );
        reg.register(
            "confluence_get_page",
            "Get content of a specific Confluence page by its ID, or by its title and space key.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "page_id", Map.of("type", "string", "description", "Confluence page ID (numeric)"),
                    "title", Map.of("type", "string", "description", "Page title (use with space_key)"),
                    "space_key", Map.of("type", "string", "description", "Space key (use with title)"),
                    "include_metadata", Map.of("type", "boolean", "description", "Include page metadata", "default", true),
                    "convert_to_markdown", Map.of("type", "boolean", "description", "Convert to markdown", "default", true)
                )
            ),
            params -> confluenceTools.getPage(asMap(params)).toFuture().join()
        );
        reg.register(
            "confluence_get_page_children",
            "Get child pages of a specific Confluence page.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "parent_id", Map.of("type", "string", "description", "Parent page ID"),
                    "limit", Map.of("type", "integer", "description", "Maximum number of results", "default", 25)
                ),
                "required", java.util.List.of("parent_id")
            ),
            params -> confluenceTools.getPageChildren(asMap(params)).toFuture().join()
        );
        reg.register(
            "confluence_get_comments",
            "Get comments for a specific Confluence page.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "page_id", Map.of("type", "string", "description", "Page ID"),
                    "limit", Map.of("type", "integer", "description", "Maximum number of comments", "default", 25)
                ),
                "required", java.util.List.of("page_id")
            ),
            params -> confluenceTools.getComments(asMap(params)).toFuture().join()
        );
        reg.register(
            "confluence_get_labels",
            "Get labels for a specific Confluence page.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "page_id", Map.of("type", "string", "description", "Page ID")
                ),
                "required", java.util.List.of("page_id")
            ),
            params -> confluenceTools.getLabels(asMap(params)).toFuture().join()
        );
        reg.register(
            "confluence_add_label",
            "Add label to an existing Confluence page.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "page_id", Map.of("type", "string", "description", "Page ID"),
                    "name", Map.of("type", "string", "description", "Label name")
                ),
                "required", java.util.List.of("page_id", "name")
            ),
            params -> confluenceTools.addLabel(asMap(params)).toFuture().join()
        );
        reg.register(
            "confluence_create_page",
            "Create a new Confluence page.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "space_key", Map.of("type", "string", "description", "The key of the space to create the page in (usually a short uppercase code like 'DEV', 'TEAM', or 'DOC')"),
                    "title", Map.of("type", "string", "description", "The title of the page"),
                    "content", Map.of("type", "string", "description", "The content of the page. Format depends on content_format parameter. Can be Markdown (default), wiki markup, or storage format"),
                    "parent_id", Map.of("type", "string", "description", "(Optional) parent page ID. If provided, this page will be created as a child of the specified page"),
                    "content_format", Map.of("type", "string", "description", "(Optional) The format of the content parameter. Options: 'markdown' (default), 'wiki', or 'storage'. Wiki format uses Confluence wiki markup syntax", "default", "markdown"),
                    "enable_heading_anchors", Map.of("type", "boolean", "description", "(Optional) Whether to enable automatic heading anchor generation. Only applies when content_format is 'markdown'", "default", false)
                ),
                "required", java.util.List.of("space_key", "title", "content")
            ),
            params -> confluenceTools.createPage(asMap(params)).toFuture().join()
        );
        reg.register(
            "confluence_update_page",
            "Update an existing Confluence page.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "page_id", Map.of("type", "string", "description", "The ID of the page to update"),
                    "title", Map.of("type", "string", "description", "The new title of the page"),
                    "content", Map.of("type", "string", "description", "The new content of the page. Format depends on content_format parameter"),
                    "is_minor_edit", Map.of("type", "boolean", "description", "Whether this is a minor edit", "default", false),
                    "version_comment", Map.of("type", "string", "description", "Optional comment for this version"),
                    "parent_id", Map.of("type", "string", "description", "Optional the new parent page ID"),
                    "content_format", Map.of("type", "string", "description", "(Optional) The format of the content parameter. Options: 'markdown' (default), 'wiki', or 'storage'. Wiki format uses Confluence wiki markup syntax", "default", "markdown"),
                    "enable_heading_anchors", Map.of("type", "boolean", "description", "(Optional) Whether to enable automatic heading anchor generation. Only applies when content_format is 'markdown'", "default", false)
                ),
                "required", java.util.List.of("page_id", "title", "content")
            ),
            params -> confluenceTools.updatePage(asMap(params)).toFuture().join()
        );
        reg.register(
            "confluence_delete_page",
            "Delete an existing Confluence page.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "page_id", Map.of("type", "string", "description", "Page ID to delete")
                ),
                "required", java.util.List.of("page_id")
            ),
            params -> confluenceTools.deletePage(asMap(params)).toFuture().join()
        );
        reg.register(
            "confluence_add_comment",
            "Add a comment to a Confluence page.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "page_id", Map.of("type", "string", "description", "Page ID"),
                    "content", Map.of("type", "string", "description", "Comment text")
                ),
                "required", java.util.List.of("page_id", "content")
            ),
            params -> confluenceTools.addComment(asMap(params)).toFuture().join()
        );
        reg.register(
            "confluence_search_user",
            "Search Confluence users using CQL.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "query", Map.of("type", "string", "description", "Search query (username, display name, or email)"),
                    "limit", Map.of("type", "integer", "description", "Maximum number of results", "default", 10)
                ),
                "required", java.util.List.of("query")
            ),
            params -> confluenceTools.searchUser(asMap(params)).toFuture().join()
        );
    }
    
    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object params) {
        return params instanceof Map ? (Map<String, Object>) params : Map.of();
    }
}
