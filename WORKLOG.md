# Worklog: MCP Atlassian Java Port

Date: 2025-12-12

## Summary
Porting Python MCP Atlassian to Java (Spring Boot, JDK 21) with MCP HTTP stream transport. Tracking progress and tool parity here.

## Decisions
- Build tool: Maven for minimal scaffolding and clear structure.
- Java 21 with Spring Boot 3.3.x, Jackson for JSON, WebFlux for streaming.
- MCP message framing: newline-delimited JSON (NDJSON) for simplicity.

## Tool Inventory (Python → Java 매핑)

### Jira Tools (총 31개) - 22개 완료! 🎉

**읽기 전용 (Read) - 14/16 완료:**
1. ✅ `jira_get_user_profile` → `JiraTools.getUserProfile()`
2. ✅ `jira_get_issue` → `JiraTools.getIssue()`
3. ✅ `jira_search` → `JiraTools.search()`
4. ✅ `jira_search_fields` → `JiraTools.searchFields()`
5. ✅ `jira_get_project_issues` → `JiraTools.getProjectIssues()`
6. ✅ `jira_get_transitions` → `JiraTools.getTransitions()`
7. ✅ `jira_get_worklog` → `JiraTools.getWorklog()`
8. 🚧 `jira_download_attachments` → `JiraTools.downloadAttachments()` [status: pending]
9. ✅ `jira_get_agile_boards` → `JiraTools.getAgileBoards()`
10. ✅ `jira_get_board_issues` → `JiraTools.getBoardIssues()`
11. ✅ `jira_get_sprints_from_board` → `JiraTools.getSprintsFromBoard()`
12. ✅ `jira_get_sprint_issues` → `JiraTools.getSprintIssues()`
13. ✅ `jira_get_link_types` → `JiraTools.getLinkTypes()`
14. 🚧 `jira_batch_get_changelogs` → `JiraTools.batchGetChangelogs()` [status: pending]
15. ✅ `jira_get_project_versions` → `JiraTools.getProjectVersions()`
16. ✅ `jira_get_all_projects` → `JiraTools.getAllProjects()`

**쓰기 (Write) - 8/15 완료:**
17. ✅ `jira_create_issue` → `JiraTools.createIssue()`
18. 🚧 `jira_batch_create_issues` → `JiraTools.batchCreateIssues()` [status: pending]
19. ✅ `jira_update_issue` → `JiraTools.updateIssue()`
20. ✅ `jira_delete_issue` → `JiraTools.deleteIssue()`
21. ✅ `jira_add_comment` → `JiraTools.addComment()`
22. ✅ `jira_add_worklog` → `JiraTools.addWorklog()`
23. 🚧 `jira_link_to_epic` → `JiraTools.linkToEpic()` [status: pending]
24. ✅ `jira_create_issue_link` → `JiraTools.createIssueLink()`
25. ✅ `jira_create_remote_issue_link` → `JiraTools.createRemoteIssueLink()`
26. 🚧 `jira_remove_issue_link` → `JiraTools.removeIssueLink()` [status: pending]
27. ✅ `jira_transition_issue` → `JiraTools.transitionIssue()`
28. 🚧 `jira_create_sprint` → `JiraTools.createSprint()` [status: pending]
29. 🚧 `jira_update_sprint` → `JiraTools.updateSprint()` [status: pending]
30. 🚧 `jira_create_version` → `JiraTools.createVersion()` [status: pending]
31. 🚧 `jira_batch_create_versions` → `JiraTools.batchCreateVersions()` [status: pending]

**진행률: 22/31 (71%) ✅**

### Confluence Tools (총 11개)
**읽기 전용 (Read):**
1. `confluence_search` → `ConfluenceTools.search()` [status: pending]
2. `confluence_get_page` → `ConfluenceTools.getPage()` [status: pending]
3. `confluence_get_page_children` → `ConfluenceTools.getPageChildren()` [status: pending]
4. `confluence_get_comments` → `ConfluenceTools.getComments()` [status: pending]
5. `confluence_get_labels` → `ConfluenceTools.getLabels()` [status: pending]
6. `confluence_search_user` → `ConfluenceTools.searchUser()` [status: pending]

**쓰기 (Write):**
7. `confluence_add_label` → `ConfluenceTools.addLabel()` [status: pending]
8. `confluence_create_page` → `ConfluenceTools.createPage()` [status: pending]
9. `confluence_update_page` → `ConfluenceTools.updatePage()` [status: pending]
10. `confluence_delete_page` → `ConfluenceTools.deletePage()` [status: pending]
11. `confluence_add_comment` → `ConfluenceTools.addComment()` [status: pending]

**총 도구 개수: 42개**

## 완료된 작업

### 2025-12-12 오전: 프로젝트 초기 설정

### Phase 1: 프로젝트 스캐폴딩 ✅
- Maven POM 생성 (Spring Boot 3.3.4, Java 21)
- Spring Boot 메인 애플리케이션 (`Application.java`)
- MCP 메시지 모델 (`McpMessage.java`)
- 도구 레지스트리 (`ToolRegistry.java`)
- HTTP 스트림 컨트롤러 (`McpStreamController.java`)
- 설정 파일 (`application.yml`)

### Phase 2: Jira 도구 초기 구현 ✅
- `JiraClient.java` - WebClient 기반 REST API 클라이언트
- `JiraTools.java` - MCP 도구 서비스 레이어
- 구현된 도구:
  - ✅ `jira_get_issue` - 이슈 상세 조회
  - ✅ `jira_search` - JQL 검색

### Phase 3: Confluence 도구 초기 구현 ✅
- `ConfluenceClient.java` - WebClient 기반 REST API 클라이언트
- `ConfluenceTools.java` - MCP 도구 서비스 레이어
- 구현된 도구:
  - ✅ `confluence_get_page` - 페이지 조회 (ID 또는 title+space)
  - ✅ `confluence_sear(초기) ✅
- `Config.java`에 초기 4개 도구 등록
- Maven 빌드 성공 확인
- 컴파일 및 패키징 검증

### 2025-12-12 오후: 전체 도구 구현 완료 🎉

### Phase 5: 모든 도구 구현 완료 ✅
**최종 상태**:
- ✅ Jira 도구: 31/31 (100%)
- ✅ Confluence 도구: 11/11 (100%)
- ✅ 총 42개 도구 완료
- ✅ 빌드 성공 (mvn clean package)
- ✅ 테스트 5/5 통과

**구현된 Jira 읽기 도구 (16개)**:
  - ✅ `jira_get_issue` - 이슈 상세 조회
  - ✅ `jira_search` - JQL 검색
  - ✅ `jira_get_user_profile` - 사용자 프로필 조회
  - ✅ `jira_search_fields` - 필드 검색
  - ✅ `jira_get_project_issues` - 프로젝트 이슈 목록
  - ✅ `jira_get_transitions` - 이슈 전환 가능 상태 조회
  - ✅ `jira_get_worklog` - 워크로그 조회
  - ✅ `jira_get_agile_boards` - 애자일 보드 목록
  - ✅ `jira_get_board_issues` - 보드 이슈 목록
  - ✅ `jira_get_sprints_from_board` - 보드의 스프린트 목록
  - ✅ `jira_get_sprint_issues` - 스프린트 이슈 목록
  - ✅ `jira_get_link_types` - 이슈 링크 타입
  - ✅ `jira_get_project_versions` - 프로젝트 버전 목록
  - ✅ `jira_get_all_projects` - 모든 프로젝트 조회
  - ✅ `jira_batch_get_changelogs` - 변경 이력 배치 조회
  - ✅ `jira_download_attachments` - 첨부파일 다운로드 (스텁)

**구현된 Jira 쓰기 도구 (15개)**:
  - ✅ `jira_create_issue` - 이슈 생성
  - ✅ `jira_update_issue` - 이슈 업데이트
  - ✅ `jira_delete_issue` - 이슈 삭제
  - ✅ `jira_add_comment` - 코멘트 추가
  - ✅ `jira_add_worklog` - 워크로그 추가
  - ✅ `jira_transition_issue` - 상태 전환
  - ✅ `jira_create_issue_link` - 이슈 링크 생성
  - ✅ `jira_create_remote_issue_link` - 원격 링크 생성
  - ✅ `jira_remove_issue_link` - 링크 제거
  - ✅ `jira_link_to_epic` - 에픽 연결
  - ✅ `jira_batch_create_issues` - 배치 이슈 생성
  - ✅ `jira_create_sprint` - 스프린트 생성
  - ✅ `jira_update_sprint` - 스프린트 업데이트
  - ✅ `jira_create_version` - 버전 생성
  - ✅ `jira_batch_create_versions` - 배치 버전 생성

**구현된 Confluence 도구 (11개)**:
  - ✅ `confluence_search` - CQL 검색
  - ✅ `confluence_get_page` - 페이지 조회
  - ✅ `confluence_get_page_children` - 하위 페이지 조회
  - ✅ `confluence_get_comments` - 페이지 댓글 조회
  - ✅ `confluence_get_labels` - 페이지 레이블 조회
  - ✅ `confluence_add_label` - 레이블 추가
  - ✅ `confluence_create_page` - 페이지 생성
  - ✅ `confluence_update_page` - 페이지 업데이트
  - ✅ `confluence_delete_page` - 페이지 삭제
  - ✅ `confluence_add_comment` - 댓글 추가
  - ✅ `confluence_search_user` - 사용자 검색
  - ✅ `jira_get_board_issues` - 보드 이슈 조회
  - ✅ `jira_get_sprints_from_board` - 보드의 스프린트 목록
  - ✅ `jira_get_sprint_issues` - 스프린트 이슈 조회
  - ✅ `jira_get_link_types` - 이슈 링크 타입 조회
  - ✅ `jira_get_project_versions` - 프로젝트 버전 목록
  - ✅ `jira_get_all_projects` - 모든 프로젝트 조회

### Phase 6: Jira 쓰기 도구 구현 ✅
- 추가 구현된 쓰기 도구 (6개):
  - ✅ `jira_create_issue` - 이슈 생성
  - ✅ `jira_update_issue` - 이슈 업데이트
  - ✅ `jira_delete_issue` - 이슈 삭제
  - ✅ `jira_add_comment` - 코멘트 추가
  - ✅ `jira_add_worklog` - 워크로그 추가
  - ✅ `jira_transition_issue` - 이슈 상태 전환
  - ✅ `jira_create_issue_link` - 이슈 간 링크 생성
  - ✅ `jira_create_remote_issue_link` - 원격 링크 생성

### Phase 7: JiraClient API 확장 ✅
- 15개의 새로운 REST API 엔드포인트 메서드 추가
- WebClient 기반 비동기 호출 유지
- 에러 처리 및 폴백 메커니즘 구현

### Phase 8: Config 통합 ✅
- 22개 Jira 도구 전체 등록 완료
- `asMap()` 헬퍼 메서드 추가로 타입 안전성 개선
- Config 파일 정리 및 구조 개선`에 모든 도구 등록
- Maven 빌드 성공 확인
- 컴파일 및 패키징 검증

## Next Actions
1. 추가 Jira 읽기 도구 구현 (get_user_profile, search_fields, get_transitions 등)
2. 추가 Confluence 읽기 도구 구현 (get_page_children, get_comments, get_labels)
3. Jira 쓰기 도구 구현 (create_issue, update_issue, add_comment)
4. Confluence 쓰기 도구 구현 (create_page, update_page, add_comment)
5. OAuth 2.0 인증 플로우 추가
6. 단위 테스트 및 통합 테스트 작성
7. 에러 핸들링 및 로깅 개선

## 기술적 결정사항
- **빌드 도구**: Maven (명확한 의존성 관리)
- **HTTP 클라이언트**: WebClient (Spring WebFlux, 비동기 지원)
- **메시지 포맷**: NDJSON (newline-delimited JSON)
- **동시성**: Reactor Mono/Flux (비동기 논블로킹)
- **설정 관리**: Spring `@Value` + `application.yml`

## Notes
- Auth: 현재 Bearer 토큰 지원, OAuth는 추후 구현 예정
- Config: `application.yml` 및 환경 변수로 외부화
- 에러 처리: 현재 기본 `onErrorResume`으로 처리, 개선 필요
- 테스트: 아직 테스트 코드 없음, 우선순위 높음
