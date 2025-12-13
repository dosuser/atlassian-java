package com.atlassian.mcp.server;

import com.atlassian.mcp.auth.AtlassianClientFactory;
import com.atlassian.mcp.core.ToolRegistry;
import com.atlassian.mcp.confluence.ConfluenceTools;
import com.atlassian.mcp.jira.JiraTools;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * MCP 도구 설정.
 * Python dependencies.py와 동일한 역할: 요청별로 Client를 동적 생성.
 */
@Configuration
public class Config {
    
    private final AtlassianClientFactory clientFactory;
    
    public Config(AtlassianClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }
    
    @Bean
    public JiraTools jiraTools() {
        // Python의 get_jira_fetcher()처럼 Supplier로 Client 전달
        return new JiraTools(clientFactory::createJiraClient);
    }
    
    @Bean
    public ConfluenceTools confluenceTools() {
        // Python의 get_confluence_fetcher()처럼 Supplier로 Client 전달
        return new ConfluenceTools(clientFactory::createConfluenceClient);
    }
    
    @Bean
    public ToolRegistry toolRegistry(JiraTools jiraTools, ConfluenceTools confluenceTools) {
        ToolRegistry reg = new ToolRegistry();
        
        // 샘플 도구 (테스트용)
        reg.register("utils_echo", params -> params);
        
        // Jira 읽기 도구
        reg.register("jira_get_issue", params -> jiraTools.getIssue(asMap(params)).toFuture().join());
        reg.register("jira_search", params -> jiraTools.search(asMap(params)).toFuture().join());
        reg.register("jira_get_user_profile", params -> jiraTools.getUserProfile(asMap(params)).toFuture().join());
        reg.register("jira_search_fields", params -> jiraTools.searchFields(asMap(params)).toFuture().join());
        reg.register("jira_get_project_issues", params -> jiraTools.getProjectIssues(asMap(params)).toFuture().join());
        reg.register("jira_get_transitions", params -> jiraTools.getTransitions(asMap(params)).toFuture().join());
        reg.register("jira_get_worklog", params -> jiraTools.getWorklog(asMap(params)).toFuture().join());
        reg.register("jira_get_agile_boards", params -> jiraTools.getAgileBoards(asMap(params)).toFuture().join());
        reg.register("jira_get_board_issues", params -> jiraTools.getBoardIssues(asMap(params)).toFuture().join());
        reg.register("jira_get_sprints_from_board", params -> jiraTools.getSprintsFromBoard(asMap(params)).toFuture().join());
        reg.register("jira_get_sprint_issues", params -> jiraTools.getSprintIssues(asMap(params)).toFuture().join());
        reg.register("jira_get_link_types", params -> jiraTools.getLinkTypes(asMap(params)).toFuture().join());
        reg.register("jira_get_project_versions", params -> jiraTools.getProjectVersions(asMap(params)).toFuture().join());
        reg.register("jira_get_all_projects", params -> jiraTools.getAllProjects(asMap(params)).toFuture().join());
        
        // Jira 쓰기 도구
        reg.register("jira_create_issue", params -> jiraTools.createIssue(asMap(params)).toFuture().join());
        reg.register("jira_update_issue", params -> jiraTools.updateIssue(asMap(params)).toFuture().join());
        reg.register("jira_delete_issue", params -> jiraTools.deleteIssue(asMap(params)).toFuture().join());
        reg.register("jira_add_comment", params -> jiraTools.addComment(asMap(params)).toFuture().join());
        reg.register("jira_add_worklog", params -> jiraTools.addWorklog(asMap(params)).toFuture().join());
        reg.register("jira_transition_issue", params -> jiraTools.transitionIssue(asMap(params)).toFuture().join());
        reg.register("jira_create_issue_link", params -> jiraTools.createIssueLink(asMap(params)).toFuture().join());
        reg.register("jira_create_remote_issue_link", params -> jiraTools.createRemoteIssueLink(asMap(params)).toFuture().join());
        reg.register("jira_remove_issue_link", params -> jiraTools.removeIssueLink(asMap(params)).toFuture().join());
        reg.register("jira_link_to_epic", params -> jiraTools.linkToEpic(asMap(params)).toFuture().join());
        reg.register("jira_batch_create_issues", params -> jiraTools.batchCreateIssues(asMap(params)).toFuture().join());
        reg.register("jira_create_sprint", params -> jiraTools.createSprint(asMap(params)).toFuture().join());
        reg.register("jira_update_sprint", params -> jiraTools.updateSprint(asMap(params)).toFuture().join());
        reg.register("jira_create_version", params -> jiraTools.createVersion(asMap(params)).toFuture().join());
        reg.register("jira_batch_create_versions", params -> jiraTools.batchCreateVersions(asMap(params)).toFuture().join());
        reg.register("jira_batch_get_changelogs", params -> jiraTools.batchGetChangelogs(asMap(params)).toFuture().join());
        reg.register("jira_download_attachments", params -> jiraTools.downloadAttachments(asMap(params)).toFuture().join());
        
        // Confluence 도구 등록
        reg.register("confluence_get_page", params -> confluenceTools.getPage(asMap(params)).toFuture().join());
        reg.register("confluence_search", params -> confluenceTools.search(asMap(params)).toFuture().join());
        reg.register("confluence_get_page_children", params -> confluenceTools.getPageChildren(asMap(params)).toFuture().join());
        reg.register("confluence_get_comments", params -> confluenceTools.getComments(asMap(params)).toFuture().join());
        reg.register("confluence_get_labels", params -> confluenceTools.getLabels(asMap(params)).toFuture().join());
        reg.register("confluence_add_label", params -> confluenceTools.addLabel(asMap(params)).toFuture().join());
        reg.register("confluence_create_page", params -> confluenceTools.createPage(asMap(params)).toFuture().join());
        reg.register("confluence_update_page", params -> confluenceTools.updatePage(asMap(params)).toFuture().join());
        reg.register("confluence_delete_page", params -> confluenceTools.deletePage(asMap(params)).toFuture().join());
        reg.register("confluence_add_comment", params -> confluenceTools.addComment(asMap(params)).toFuture().join());
        reg.register("confluence_search_user", params -> confluenceTools.searchUser(asMap(params)).toFuture().join());
        
        return reg;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object params) {
        return params instanceof Map ? (Map<String, Object>) params : Map.of();
    }
}
