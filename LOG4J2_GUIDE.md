# Log4j2 설정 가이드

## 목차
1. [기본 설정](#기본-설정)
2. [외부 라이브러리 추가](#외부-라이브러리-추가)
3. [커스텀 Log4j2 설정](#커스텀-log4j2-설정)
4. [로그 레벨 설정](#로그-레벨-설정)
5. [JWT 감사 로그](#jwt-감사-로그)
6. [트러블슈팅](#트러블슈팅)

---

## 기본 설정

### 기본 Log4j2 사용
별도 설정 없이 서버를 실행하면 기본 `log4j2-spring.xml` 설정이 사용됩니다.

```bash
mvn spring-boot:run
```

### 로그 파일 위치
- **기본 경로**: `logs/` 디렉토리
- **전체 로그**: `logs/atlassian-mcp.log`
- **에러 로그**: `logs/error.log`
- **JWT 감사 로그**: `logs/jwt-audit.log` (JWT 모드 전용)

### 로그 파일 정책
- **롤링**: 매일 자정 또는 100MB 도달 시
- **보관**: 최근 30개 파일
- **압축**: 이전 로그는 gzip으로 압축

---

## 외부 라이브러리 추가

### lib 폴더 사용
프로젝트 루트에 `lib/` 폴더를 생성하고 외부 JAR 파일을 배치하면 자동으로 로드됩니다.

```bash
# lib 폴더 구조
atlassian-java/
├── lib/
│   ├── custom-log4j2-appender.jar
│   ├── kafka-appender.jar
│   └── splunk-appender.jar
├── pom.xml
└── src/
```

### 서버 실행 시 자동 로드
```bash
# lib 폴더의 모든 JAR이 classpath에 추가됨
mvn spring-boot:run
```

### 패키징 시 포함
```bash
# target/lib/ 폴더에 모든 의존성 복사됨
mvn clean package

# 실행 시 lib 폴더 자동 로드
java -jar target/atlassian-mcp-java-0.1.0-SNAPSHOT.jar
```

---

## 커스텀 Log4j2 설정

### 외부 설정 파일 사용

#### 1. 커스텀 설정 파일 생성
```xml
<!-- /path/to/custom-log4j2.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{ISO8601} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
        
        <!-- Kafka Appender 예시 (외부 라이브러리 필요) -->
        <Kafka name="Kafka" topic="atlassian-logs">
            <PatternLayout pattern="%d{ISO8601} %-5level %logger{36} - %msg%n"/>
            <Property name="bootstrap.servers">localhost:9092</Property>
        </Kafka>
    </Appenders>
    
    <Loggers>
        <Root level="INFO">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="Kafka"/>
        </Root>
    </Loggers>
</Configuration>
```

#### 2. 설정 파일 경로 지정

**방법 1: 환경변수**
```bash
export LOGGING_CONFIG=/path/to/custom-log4j2.xml
mvn spring-boot:run
```

**방법 2: application.yml**
```yaml
logging:
  config: /path/to/custom-log4j2.xml
```

**방법 3: 시스템 속성**
```bash
mvn spring-boot:run -Dlogging.config=/path/to/custom-log4j2.xml
```

**방법 4: JAR 실행 시**
```bash
java -jar -Dlogging.config=/path/to/custom-log4j2.xml \
  target/atlassian-mcp-java-0.1.0-SNAPSHOT.jar
```

---

## 로그 레벨 설정

### 환경변수로 설정
```bash
# application.yml의 로그 레벨 오버라이드
export LOGGING_LEVEL_COM_ATLASSIAN_MCP=DEBUG
mvn spring-boot:run
```

### application.yml에서 설정
```yaml
logging:
  level:
    com.atlassian.mcp: DEBUG
    com.atlassian.mcp.auth: INFO
    org.springframework: WARN
```

### 런타임 변경 (log4j2.xml에서 설정 필요)
```xml
<!-- monitorInterval로 자동 리로드 설정 (초 단위) -->
<Configuration status="WARN" monitorInterval="30">
```

설정 파일을 수정하면 30초 후 자동으로 반영됩니다.

---

## JWT 감사 로그

### JWT 모드 활성화 시 자동 기록
```bash
export SECURITY_MODE=jwt
export JWT_SECRET=your-secret-key
mvn spring-boot:run
```

### 로그 형식
```
2025-12-12 20:30:45.123 [http-nio-8080-exec-1] INFO  c.a.m.a.JwtAuditLogger - JWT_AUDIT | user=user123 | time=2025-12-12 20:30:45.123 | tool=jira_get_issue | params={issue_key=PROJ-123}
```

### 로그 항목
- **user**: JWT subject (사용자 ID)
- **time**: 호출 시간 (yyyy-MM-dd HH:mm:ss.SSS)
- **tool**: MCP 도구명
- **params**: 도구 파라미터 (민감 정보 마스킹 처리됨)

### 민감 정보 자동 마스킹
- `password`, `token`, `secret`, `key` 필드는 자동으로 `***`로 마스킹
- 500자 이상의 파라미터는 자동으로 자름

### 기본 모드에서는 기록 안 됨
```bash
# SECURITY_MODE=none (기본값)
# JWT 감사 로그 기록 안 됨
mvn spring-boot:run
```

---

## 인증 실패 로그

### ERROR 레벨로 자동 기록
인증 실패는 **항상 ERROR 레벨**로 로그에 기록됩니다:

```
2025-12-12 20:35:12.456 [http-nio-8080-exec-2] ERROR c.a.m.a.JwtAuthenticationFilter - JWT validation failed: JWT expired at 2025-12-11T15:30:00Z
2025-12-12 20:35:15.789 [http-nio-8080-exec-3] ERROR c.a.m.a.JwtAuthenticationFilter - Missing or invalid Authorization header
```

### 별도 에러 로그 파일
- **파일**: `logs/error.log`
- **내용**: ERROR 레벨 이상의 모든 로그
- **롤링**: 매일 자정 또는 50MB 도달 시

---

## 고급 설정 예시

### 1. Splunk로 전송
```xml
<!-- lib/splunk-logging-*.jar 필요 -->
<Appenders>
    <Socket name="Splunk" host="splunk.example.com" port="9997" protocol="TCP">
        <PatternLayout pattern="%d{ISO8601} %-5level %logger{36} - %msg%n"/>
    </Socket>
</Appenders>

<Loggers>
    <Logger name="com.atlassian.mcp.auth.JwtAuditLogger" level="INFO">
        <AppenderRef ref="Splunk"/>
    </Logger>
</Loggers>
```

### 2. Elasticsearch로 전송
```xml
<!-- lib/log4j2-elasticsearch-*.jar 필요 -->
<Appenders>
    <Elasticsearch name="Elastic" 
                   serverUris="http://localhost:9200"
                   indexName="atlassian-mcp">
        <JsonLayout compact="true" eventEol="true"/>
    </Elasticsearch>
</Appenders>
```

### 3. 비동기 로깅 (성능 향상)
```xml
<Appenders>
    <Async name="AsyncFile" blocking="false">
        <AppenderRef ref="RollingFile"/>
    </Async>
</Appenders>

<Loggers>
    <Root level="INFO">
        <AppenderRef ref="AsyncFile"/>
    </Root>
</Loggers>
```

---

## 트러블슈팅

### 로그 파일이 생성되지 않음
```bash
# logs 디렉토리 권한 확인
ls -ld logs/
# 권한이 없으면 생성
mkdir -p logs && chmod 755 logs
```

### 커스텀 설정 파일이 적용되지 않음
```bash
# 설정 파일 경로 확인
mvn spring-boot:run -Dlogging.config=/absolute/path/to/log4j2.xml -X

# 출력에서 "Loading configuration" 메시지 확인
```

### 외부 라이브러리가 로드되지 않음
```bash
# lib 폴더 확인
ls -la lib/

# JAR 파일 실행 시 loader.path 확인
java -Dloader.path=lib/ -jar target/*.jar
```

### JWT 감사 로그가 기록되지 않음
```bash
# SECURITY_MODE 확인
echo $SECURITY_MODE  # jwt 여야 함

# 로그 레벨 확인 (INFO 이상이어야 함)
# application.yml에서:
logging:
  level:
    com.atlassian.mcp.auth.JwtAuditLogger: INFO
```

### 로그 레벨 변경이 반영되지 않음
```xml
<!-- log4j2.xml의 monitorInterval 확인 -->
<Configuration status="WARN" monitorInterval="30">
  <!-- 30초마다 자동 리로드 -->
</Configuration>
```

---

## 빠른 시작 체크리스트

- [ ] 기본 로그 확인: `logs/atlassian-mcp.log`
- [ ] JWT 모드: `SECURITY_MODE=jwt` 설정
- [ ] 커스텀 설정: `LOGGING_CONFIG` 환경변수 설정
- [ ] 외부 라이브러리: `lib/` 폴더에 JAR 파일 배치
- [ ] 로그 레벨: `application.yml` 또는 환경변수로 조정

---

## 예제 실행 명령어

### 기본 모드 (JWT 감사 로그 없음)
```bash
mvn spring-boot:run
```

### JWT 모드 (감사 로그 활성화)
```bash
export SECURITY_MODE=jwt
export JWT_SECRET=my-secret-key
mvn spring-boot:run
```

### 커스텀 Log4j2 설정
```bash
export LOGGING_CONFIG=/path/to/custom-log4j2.xml
mvn spring-boot:run
```

### 프로덕션 배포
```bash
java -jar \
  -Dlogging.config=/etc/atlassian-mcp/log4j2.xml \
  -Dloader.path=/opt/atlassian-mcp/lib/ \
  target/atlassian-mcp-java-0.1.0-SNAPSHOT.jar
```
