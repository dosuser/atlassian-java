#!/bin/bash

# MCP Tools Test Runner
# 개별 도구를 간단하게 테스트할 수 있는 스크립트

echo "======================================"
echo "MCP Atlassian Java - Tool Tester"
echo "======================================"
echo ""

# 1. Echo 테스트
echo "1. Testing utils_echo..."
mvn test -Dtest=McpIntegrationTest#testUtilsEcho -q
echo ""

# 2. Confluence 페이지 조회
echo "2. Testing confluence_get_page..."
mvn test -Dtest=ConfluenceToolsTest#testGetPage -q
echo ""

# 3. Confluence 검색
echo "3. Testing confluence_search..."
mvn test -Dtest=ConfluenceToolsTest#testSearch -q
echo ""

# 4. Jira 검색
echo "4. Testing jira_search..."
mvn test -Dtest=JiraToolsTest#testSearch -q
echo ""

# 5. Jira 프로젝트 목록
echo "5. Testing jira_get_all_projects..."
mvn test -Dtest=JiraToolsTest#testGetAllProjects -q
echo ""

# 6. 전체 통합 테스트
echo "6. Running integration tests..."
mvn test -Dtest=McpIntegrationTest -q
echo ""

echo "======================================"
echo "All tests completed!"
echo "======================================"
