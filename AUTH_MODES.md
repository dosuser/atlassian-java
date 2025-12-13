# 인증 모드 설정 가이드

> **중요**: 이 프로젝트는 Spring Security를 사용하지 않습니다. 
> 모든 인증은 서블릿 필터(`jakarta.servlet.Filter`)로 구현되어 경량화되었습니다.

## 두 가지 인증 모드

### 1. 기본 모드 (security.mode=none)
- **단순한 Bearer 토큰 방식**
- Python mcp-atlassian과 동일한 방식
- Jira와 Confluence에 동일한 토큰 사용

#### 사용법
```bash
# 환경변수 설정 (또는 .env 파일)
SECURITY_MODE=none  # 생략 가능 (기본값)

# 서버 실행
mvn spring-boot:run

# API 호출
curl -X POST http://localhost:8080/mcp \
  -H "Authorization: Bearer YOUR_ATLASSIAN_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{"id":"1","type":"REQUEST","method":"jira_get_issue","params":{"issue_key":"PROJ-123"}}'
```

### 2. JWT 모드 (security.mode=jwt)
- **JWT 기반 인증 + 별도 서비스 토큰**
- JWT로 사용자 인증, 별도 헤더로 Jira/Confluence 토큰 전달
- 완전히 분리된 인증 체계

#### 사용법
```bash
# 환경변수 설정 (또는 .env 파일)
SECURITY_MODE=jwt
JWT_SECRET=my-super-secret-jwt-signing-key-at-least-32-bytes-long

# 서버 실행
mvn spring-boot:run

# API 호출
curl -X POST http://localhost:8080/mcp \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "JIRA_TOKEN: YOUR_JIRA_TOKEN_HERE" \
  -H "CONFLUENCE_TOKEN: YOUR_CONFLUENCE_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{"id":"1","type":"REQUEST","method":"jira_get_issue","params":{"issue_key":"PROJ-123"}}'
```

## 환경변수 (.env 파일)

### 기본 모드
```bash
# .env
SECURITY_MODE=none
```

### JWT 모드
```bash
# .env
SECURITY_MODE=jwt
JWT_SECRET=your-secret-key-here-at-least-32-bytes-long-for-hs256
```

## JWT 토큰 생성 (개발/테스트용)

```bash
# Python으로 JWT 생성
pip install pyjwt

python3 << 'EOF'
import jwt
import time

payload = {
    'sub': 'user123',  # 사용자 ID
    'iat': int(time.time()),
    'exp': int(time.time()) + 86400  # 24시간 후 만료
}

secret = 'my-super-secret-jwt-signing-key-at-least-32-bytes-long'
token = jwt.encode(payload, secret, algorithm='HS256')
print(token)
EOF
```

## 아키텍처 비교

### 기본 모드 (none)
```
클라이언트
   ↓ Authorization: Bearer <atlassian_token>
BearerTokenFilter
   ↓ request.setAttribute("jira_token", token)
   ↓ request.setAttribute("confluence_token", token)
AtlassianClientFactory
   ↓ 동일한 토큰 사용
JiraClient / ConfluenceClient
```

### JWT 모드 (jwt)
```
클라이언트
   ↓ Authorization: Bearer <JWT>
   ↓ JIRA_TOKEN: <jira_token>
   ↓ CONFLUENCE_TOKEN: <confluence_token>
JwtAuthenticationFilter
   ↓ JWT 검증 (서명, 만료 확인)
   ↓ request.setAttribute("user_id", sub)
   ↓ request.setAttribute("jira_token", jira_token)
   ↓ request.setAttribute("confluence_token", confluence_token)
AtlassianClientFactory
   ↓ 각 서비스별 토큰 사용
JiraClient / ConfluenceClient
```

## 보안 차이점

| 항목 | 기본 모드 | JWT 모드 |
|------|----------|---------|
| 인증 방식 | Bearer 토큰 | JWT 서명 검증 |
| 토큰 관리 | 단일 토큰 | JWT + 서비스별 토큰 |
| 사용자 식별 | 없음 | JWT sub claim |
| 만료 시간 | Atlassian 토큰 만료 | JWT exp + Atlassian 토큰 만료 |
| 보안 수준 | 낮음 | 높음 |
| 적합한 용도 | 개발/테스트 | 프로덕션 |

## 서블릿 필터 기반 인증

**Spring Security 불사용** - 모든 인증은 `jakarta.servlet.Filter` 기반으로 구현

### 필터 구조

```java
// 기본 모드일 때만 활성화
@Component
@ConditionalOnProperty(name = "app.security.mode", havingValue = "none", matchIfMissing = true)
public class BearerTokenFilter extends OncePerRequestFilter { ... }

// JWT 모드일 때만 활성화
@Component
@ConditionalOnProperty(name = "app.security.mode", havingValue = "jwt")
public class JwtAuthenticationFilter extends OncePerRequestFilter { ... }
```

### 인증 흐름

1. **서블릿 필터**가 요청 헤더에서 토큰 추출
2. **Request Attribute**에 인증 정보 저장:
   - `jira_token`: Jira API 토큰
   - `confluence_token`: Confluence API 토큰
   - `user_id`: 사용자 ID (JWT 모드만)
   - `auth_mode`: 인증 모드 ("none" or "jwt")
3. **AtlassianClientFactory**가 Request Attribute에서 토큰 조회
4. 각 요청마다 독립적인 클라이언트 인스턴스 생성

**장점:**
- Spring Security 의존성 제거로 빌드 크기 감소
- 서블릿 레벨의 간단한 인증으로 디버깅 용이
- 두 필터는 동시에 활성화되지 않음 → 완전히 분리된 인증 체계

## 문제 해결

### JWT 모드에서 "JWT secret is required" 에러
```bash
# JWT_SECRET 환경변수 설정 필요
export JWT_SECRET=your-secret-key-here
mvn spring-boot:run
```

### "No Jira token" 에러
```bash
# JWT 모드: JIRA_TOKEN 헤더 필요
curl -H "JIRA_TOKEN: <token>" ...

# 기본 모드: Authorization Bearer 헤더 필요
curl -H "Authorization: Bearer <token>" ...
```

### JWT 검증 실패
- JWT secret이 토큰 생성 시와 동일한지 확인
- JWT가 만료되지 않았는지 확인 (exp claim)
- JWT에 sub claim이 있는지 확인
