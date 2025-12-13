테스트 방법
vs code에 mcp가 등록되어 있다. #
테스트 대상 문서
https://wiki.navercorp.com/spaces/~KR18723/pages/4548210001/test
(토큰은 .env 파일에서 관리)

jira는 autobid 프로젝트 하위로 테스트해
jira 테스트 기준 https://jira.navercorp.com/browse/GFA_SERVING-352

## Confluence (Wiki) 테스트 작업

### 1. 페이지 조회 (Read)
- [x] confluence_get_page: 페이지 ID로 특정 페이지 내용 가져오기 ✅ (ID: 4548210001)
- [x] confluence_get_page: 페이지 제목 + Space Key로 페이지 가져오기 ✅ (title: test, space: ~KR18723)
- [x] confluence_get_page: 마크다운 변환 옵션 테스트 ✅
- [x] confluence_get_page_children: 하위 페이지 목록 조회 (parent_id 파라미터 사용)

### 2. 검색 (Search)
- [x] confluence_search: CQL로 페이지 검색 ✅ (type=page AND space=~KR18723 -> 83건 검색됨, CQL quote 처리 수정됨)
- [x] confluence_search: 단순 텍스트 검색 ✅ (query: test, 211810건 검색됨)
- [x] confluence_search: 특정 Space 필터링 검색 ✅ (spaces_filter: ~KR18723)
- [ ] confluence_search_user: 사용자 검색 ⚠️ (404 Not Found - 서버에서 미지원)

### 3. 코멘트 (Comments)
- [x] confluence_get_comments: 페이지의 모든 코멘트 조회 ✅ (3개 코멘트)
- [x] confluence_add_comment: 코멘트 '안녕' 작성 ✅ (ID: 4548210127)
- [x] confluence_add_comment: 코멘트 작성 후 삭제 (삭제는 별도 API 필요) ✅ (ID: 4548210309)

### 4. 레이블 (Labels)
- [x] confluence_get_labels: 페이지의 레이블 목록 조회 ✅ (test-label, mcp-test)
- [x] confluence_add_label: 페이지에 레이블 추가 (예: 'test-label') ✅ (레이블: 'mcp-test')

### 5. 페이지 생성 및 수정 (Write)
- [x] confluence_create_page: 새 페이지 생성 (Space Key + 제목 + 내용) ✅ (ID: 4548210294 with 'content' param)
- [x] confluence_create_page: 부모 페이지 지정하여 하위 페이지 생성 ✅ (ID: 4548210299, 부모: 4548210001)
- [x] confluence_update_page: 페이지 제목/내용 수정 ✅ (ID: 4548210294, version 2, RequestContext 문제 수정됨)
- [x] confluence_delete_page: 테스트 페이지 삭제 ✅
- [x] confluence_delete_page: 전체 시나리오 테스트 (생성 후 삭제) ✅ (ID: 4548210298, atlassian2 서버)

**중요**: 서버는 Python 기준 'content' 파라미터로 변경 완료. VS Code 리로드 필요!

### 6. 통합 시나리오
1. 특정 Space에서 'test' 키워드로 페이지 검색
2. 검색된 페이지의 코멘트 조회
3. 새 코멘트 작성
4. 페이지에 'tested' 레이블 추가
5. 하위 페이지 생성
6. 생성한 하위 페이지 삭제

## Jira 테스트 작업

### 1. 이슈 조회 (Read)
- [x] jira_search: JQL로 최근 업데이트된 이슈 검색 ✅ (GFA_SERVING 프로젝트 346건)
- [x] jira_get_issue: 특정 이슈 상세 조회 ✅ (GFA_SERVING-352)
- [x] jira_search_fields: 필드 검색 (summary, status 등) ✅ (status 검색: 2개 필드)
- [x] jira_get_transitions: 이슈의 가능한 상태 전환 조회 ✅ (8개 전환)

### 2. 프로젝트 및 버전 (Read)
- [ ] jira_get_all_projects: 모든 프로젝트 목록 조회 ⚠️ (DataBufferLimitException: 262144 바이트 초과)
- [x] jira_get_project_issues: 특정 프로젝트의 이슈 목록 ✅ (GFA_SERVING: 346건)
- [x] jira_get_project_versions: 프로젝트의 버전 목록 ✅ (3개 버전)

### 3. 애자일 보드 (Read)
- [x] jira_get_agile_boards: 보드 목록 조회 ✅ (GFA_SERVING 2개 보드)
- [x] jira_get_board_issues: 특정 보드의 이슈 조회 ✅ (board_id: 1800, 338건)
- [x] jira_get_sprints_from_board: 보드의 스프린트 목록 ✅ (board_id: 1804, 1개 스프린트)
- [x] jira_get_sprint_issues: 특정 스프린트의 이슈 목록 ✅ (sprint_id: 2301, 6건)

### 4. 코멘트 및 워크로그 (Read)
- [x] jira_get_worklog: 이슈의 작업 기록 조회 ✅ (1개 워크로그)

### 5. 이슈 생성 및 수정 (Write) (GFA_SERVING 프로젝트는 "작업" 타입 사용, "Task" 아님)
- [x] jira_update_issue: 이슈 정보 수정 ✅ (GFA_SERVING-352, 204 No Content 처리 수정됨)
- [x] jira_transition_issue: 이슈 상태 전환 ✅ (GFA_SERVING-352 -> "진행중" transition_id=21, 204 처리 수정됨)
- [x] jira_add_comment: 이슈에 코멘트 추가 ✅ (GFA_SERVING-352에 코멘트 ID 11350584 추가됨)
- [x] jira_add_worklog: 작업 시간 기록 ✅ (GFA_SERVING-352에 1h 작업 시간 기록 ID 1559594)
- [ ] jira_delete_issue: 이슈 삭제

### 6. 링크 및 관계 (Write)
- [x] jira_create_issue_link: 이슈 간 링크 생성 ✅ (GFA_SERVING-352와 GFA_SERVING-392를 '관련된 이슈'로 연결)
- [x] jira_get_link_types: 사용 가능한 링크 타입 조회 ✅ (8개 타입)
- [ ] jira_remove_issue_link: 이슈 링크 제거
- [x] jira_link_to_epic: 이슈를 에픽에 연결 ✅ (GFA_SERVING-352 -> GFA_SERVING-332 Epic 연결, 204 처리 수정됨)

### 7. 버전 관리 (Write)
- [x] jira_create_version: 새 버전 생성 ✅ (MCP-TEST-VERSION-1.0, ID: 419754)
- [x] jira_batch_create_versions: 여러 버전 일괄 생성 ✅ (MCP-TEST-BATCH-v1.0/v2.0, IDs: 419755, 419756)

### 8. 일괄 작업 (Write)
- [ ] jira_batch_create_issues: 여러 이슈 일괄 생성 ⚠️ (400 Bad Request - 서버측 이슈 가능성)
- [x] jira_batch_get_changelogs: 여러 이슈의 변경 이력 조회 ✅ (GFA_SERVING-352: 9개, GFA_SERVING-392: 2개)

### 9. 코드 수정 내역

#### A. 204 No Content 파싱 에러 수정
JiraClient.java에서 POST/PUT 요청이 204 No Content를 반환하는 경우 처리:
- Line 202-209: updateIssue() - `.bodyToMono(JsonNode.class)` → `.toBodilessEntity()`
- Line 234-241: transitionIssue() - `.bodyToMono(JsonNode.class)` → `.toBodilessEntity()`
- Line 268-275: linkToEpic() - `.bodyToMono(JsonNode.class)` → `.toBodilessEntity()`

**패턴**: 204 No Content 응답을 기대하는 API는 `.toBodilessEntity().then(getIssue())`로 처리

#### B. board_id/sprint_id Integer→String 타입 변환 수정
- JiraReadToolsA.java Line 149-150: getBoardIssues()에서 board_id Integer 처리
- JiraReadToolsB.java Line 95-96: getSprintIssues()에서 sprint_id Integer 처리  
- JiraReadToolsB.java Line 119-120: getSprintsFromBoard()에서 board_id Integer 처리

**패턴**: `Object obj = params.get("id"); String id = obj instanceof Integer ? String.valueOf(obj) : (String) obj;`

#### C. CQL Space Key Quote 처리 수정
- CqlUtils.java 생성: Python의 quote_cql_identifier_if_needed 포팅
- ConfluenceTools.java Line 95: autoQuoteSpaceKeys() 호출 추가
- `space = ~KR18723` → `space = "~KR18723"` 자동 변환으로 400 Bad Request 해결
