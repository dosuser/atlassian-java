# 프로젝트 구조

```
atlassian-java/
├── src/
│   ├── main/
│   │   ├── java/com/atlassian/mcp/
│   │   │   ├── core/                    # MCP 프로토콜 핵심
│   │   │   │   ├── McpMessage.java      # 메시지 모델 (REQUEST/RESPONSE/ERROR)
│   │   │   │   └── ToolRegistry.java    # 도구 등록 및 호출 레지스트리
│   │   │   ├── jira/                    # Jira 통합
│   │   │   │   ├── JiraClient.java      # Jira REST API WebClient
│   │   │   │   └── JiraTools.java       # Jira MCP 도구 구현
│   │   │   ├── confluence/              # Confluence 통합
│   │   │   │   ├── ConfluenceClient.java  # Confluence REST API WebClient
│   │   │   │   └── ConfluenceTools.java   # Confluence MCP 도구 구현
│   │   │   └── server/                  # Spring Boot 애플리케이션
│   │   │       ├── Application.java     # 메인 진입점
│   │   │       ├── Config.java          # Bean 설정 및 도구 등록
│   │   │       └── McpStreamController.java  # HTTP Stream 엔드포인트
│   │   └── resources/
│   │       └── application.yml          # Spring 설정 파일
│   └── test/
│       └── java/com/atlassian/mcp/core/
│           ├── McpMessageTest.java      # 메시지 모델 테스트
│           └── ToolRegistryTest.java    # 레지스트리 테스트
├── pom.xml                              # Maven 빌드 설정
├── README.md                            # 사용자 문서
├── MIGRATION_PLAN.md                    # 마이그레이션 전략
├── WORKLOG.md                           # 작업 일지
└── .gitignore                           # Git 제외 파일

총 파일 수: 16개
- Java 소스: 9개
- 테스트: 2개
- 설정/문서: 5개
```

## 빌드 명령어

```bash
# 컴파일
mvn compile

# 테스트
mvn test

# 패키징
mvn package

# 실행
mvn spring-boot:run

# 클린 빌드
mvn clean package -DskipTests
```

## 엔드포인트

- **MCP Stream**: `POST http://localhost:8080/mcp/stream`
  - Content-Type: `application/x-ndjson`
  - Accept: `application/x-ndjson`

## 현재 지원 도구 (4개)

### 테스트 도구
- `utils_echo` - 입력을 그대로 반환

### Jira (2개)
- `jira_get_issue` - 이슈 조회
- `jira_search` - JQL 검색

### Confluence (2개)
- `confluence_get_page` - 페이지 조회
- `confluence_search` - CQL 검색

## 기술 스택

| 항목 | 기술 | 버전 |
|------|------|------|
| Java | OpenJDK | 21 |
| 빌드 도구 | Maven | 3.8+ |
| 프레임워크 | Spring Boot | 3.3.4 |
| 웹 | Spring WebFlux | 3.3.4 |
| JSON | Jackson | (Spring 포함) |
| 테스트 | JUnit 5 | (Spring 포함) |
| HTTP 클라이언트 | WebClient | (WebFlux 포함) |

## 다음 단계

### 우선순위 높음
1. 나머지 Jira 읽기 도구 구현 (14개)
2. 나머지 Confluence 읽기 도구 구현 (4개)
3. 에러 핸들링 개선
4. 로깅 추가

### 우선순위 중간
5. Jira 쓰기 도구 구현 (15개)
6. Confluence 쓰기 도구 구현 (5개)
7. OAuth 2.0 인증
8. 통합 테스트

### 우선순위 낮음
9. 성능 최적화
10. 문서화 확장
11. CI/CD 파이프라인
12. Docker 이미지

## 참조

- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [WebFlux 가이드](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [MCP 프로토콜 스펙](https://modelcontextprotocol.io/)
- [Jira REST API](https://developer.atlassian.com/cloud/jira/platform/rest/v3/)
- [Confluence REST API](https://developer.atlassian.com/cloud/confluence/rest/v2/)
