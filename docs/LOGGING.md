# Logging Guide

Comprehensive guide for configuring and using Log4j2 in MCP Atlassian Server.

## Table of Contents

- [Quick Start](#quick-start)
- [Default Configuration](#default-configuration)
- [Custom Configuration](#custom-configuration)
- [External Libraries](#external-libraries)
- [JWT Audit Logging](#jwt-audit-logging)
- [Authentication Error Logging](#authentication-error-logging)
- [Log Levels](#log-levels)
- [Advanced Examples](#advanced-examples)
- [Troubleshooting](#troubleshooting)

## Quick Start

### Default Logging

No configuration needed! Start the server and logs are automatically created:

```bash
mvn spring-boot:run
```

**Log Files:**
- `logs/atlassian-mcp.log` - All application logs
- `logs/error.log` - ERROR level and above
- `logs/jwt-audit.log` - JWT audit trail (JWT mode only)

### Log File Structure

```
logs/
├── atlassian-mcp.log           # Main log file
├── atlassian-mcp.2025-12-11.log.gz   # Rolled log (compressed)
├── error.log                   # Error logs
└── jwt-audit.log               # JWT audit logs (JWT mode)
```

## Default Configuration

### Built-in Settings

The server includes a default `log4j2-spring.xml` with production-ready settings:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <!-- Console output -->
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{ISO8601} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
        
        <!-- Main log file (all levels) -->
        <RollingFile name="RollingFile"
                     fileName="logs/atlassian-mcp.log"
                     filePattern="logs/atlassian-mcp.%d{yyyy-MM-dd}.log.gz">
            <PatternLayout pattern="%d{ISO8601} [%t] %-5level %logger{36} - %msg%n"/>
            <Policies>
                <TimeBasedTriggeringPolicy interval="1"/>
                <SizeBasedTriggeringPolicy size="100MB"/>
            </Policies>
            <DefaultRolloverStrategy max="30"/>
        </RollingFile>
        
        <!-- Error log file (ERROR+ only) -->
        <RollingFile name="ErrorFile"
                     fileName="logs/error.log"
                     filePattern="logs/error.%d{yyyy-MM-dd}.log.gz">
            <PatternLayout pattern="%d{ISO8601} [%t] %-5level %logger{36} - %msg%n"/>
            <ThresholdFilter level="ERROR" onMatch="ACCEPT" onMismatch="DENY"/>
            <Policies>
                <TimeBasedTriggeringPolicy interval="1"/>
                <SizeBasedTriggeringPolicy size="50MB"/>
            </Policies>
            <DefaultRolloverStrategy max="30"/>
        </RollingFile>
        
        <!-- JWT audit log file (INFO only) -->
        <RollingFile name="JwtAuditFile"
                     fileName="logs/jwt-audit.log"
                     filePattern="logs/jwt-audit.%d{yyyy-MM-dd}.log.gz">
            <PatternLayout pattern="%d{ISO8601} [%t] %-5level %logger{36} - %msg%n"/>
            <Policies>
                <TimeBasedTriggeringPolicy interval="1"/>
                <SizeBasedTriggeringPolicy size="50MB"/>
            </Policies>
            <DefaultRolloverStrategy max="30"/>
        </RollingFile>
    </Appenders>
    
    <Loggers>
        <!-- JWT Audit Logger → jwt-audit.log -->
        <Logger name="com.atlassian.mcp.auth.JwtAuditLogger" level="INFO" additivity="false">
            <AppenderRef ref="JwtAuditFile"/>
        </Logger>
        
        <!-- Auth Loggers → error.log for failures -->
        <Logger name="com.atlassian.mcp.auth" level="WARN" additivity="false">
            <AppenderRef ref="ErrorFile"/>
            <AppenderRef ref="Console"/>
        </Logger>
        
        <!-- Root Logger -->
        <Root level="INFO">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="RollingFile"/>
            <AppenderRef ref="ErrorFile"/>
        </Root>
    </Loggers>
</Configuration>
```

### Rolling Policies

- **Time-Based:** Daily rollover at midnight
- **Size-Based:** When file reaches 100MB (main) or 50MB (error/audit)
- **Compression:** Automatic gzip compression
- **Retention:** Keep 30 days of logs

## Custom Configuration

### Environment Variable

Specify a custom configuration file:

```bash
export LOGGING_CONFIG=/path/to/custom-log4j2.xml
mvn spring-boot:run
```

### System Property

```bash
mvn spring-boot:run -Dlogging.config=/path/to/custom-log4j2.xml
```

### Docker

```dockerfile
ENV LOGGING_CONFIG=/etc/mcp/log4j2.xml
```

### Kubernetes

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mcp-log-config
data:
  log4j2.xml: |
    <?xml version="1.0" encoding="UTF-8"?>
    <Configuration>
      <!-- Custom configuration -->
    </Configuration>
---
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
        - name: mcp
          env:
            - name: LOGGING_CONFIG
              value: /config/log4j2.xml
          volumeMounts:
            - name: log-config
              mountPath: /config
      volumes:
        - name: log-config
          configMap:
            name: mcp-log-config
```

## External Libraries

### lib/ Folder

Place external Log4j2 plugins in the `lib/` folder for automatic loading.

```bash
# Project structure
atlassian-java/
├── lib/
│   ├── log4j2-kafka-appender.jar
│   ├── log4j2-elasticsearch.jar
│   └── custom-appender.jar
├── pom.xml
└── src/
```

### How It Works

The Spring Boot Maven plugin is configured to load external JARs:

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <loader.path>lib/</loader.path>
    </configuration>
</plugin>
```

### Running with External Libraries

```bash
# Development
mvn spring-boot:run

# Production
java -Dloader.path=lib/ -jar target/mcp-atlassian-java-*.jar
```

## JWT Audit Logging

### Automatic Activation

JWT audit logging is automatically enabled when `SECURITY_MODE=jwt`:

```bash
export SECURITY_MODE=jwt
export JWT_SECRET=your-secret-key
mvn spring-boot:run
```

### Log Format

```
2025-12-12 20:30:45.123 [http-nio-8080-exec-1] INFO  c.a.m.a.JwtAuditLogger - JWT_AUDIT | user=user@example.com | time=2025-12-12 20:30:45.123 | tool=jira_get_issue | params={issue_key=PROJ-123}
```

### Fields

- **user**: JWT subject (user identifier)
- **time**: Request timestamp (yyyy-MM-dd HH:mm:ss.SSS)
- **tool**: MCP tool name
- **params**: Tool parameters (sensitive data masked)

### Sensitive Data Masking

Automatically masks these parameter names:
- `password` → `***`
- `token` → `***`
- `secret` → `***`
- `key` → `***`

```java
// Input params
{
  "username": "john",
  "password": "secret123",
  "project": "PROJ"
}

// Logged params
{
  "username": "john",
  "password": "***",
  "project": "PROJ"
}
```

### Parameter Truncation

Long parameters are truncated to 500 characters:

```java
// Input: Very long JQL query (800 chars)
// Logged: First 500 chars + "..."
```

### Disabling Audit Logging

Set security mode to `none`:

```bash
export SECURITY_MODE=none
mvn spring-boot:run
```

JWT audit logging is **only active in JWT mode**.

## Authentication Error Logging

### Automatic ERROR Level

All authentication failures are logged as **ERROR** level:

```
2025-12-12 20:35:12.456 [http-nio-8080-exec-2] ERROR c.a.m.a.JwtAuthenticationFilter - JWT validation failed: JWT expired at 2025-12-11T15:30:00Z

2025-12-12 20:35:15.789 [http-nio-8080-exec-3] ERROR c.a.m.a.BearerTokenFilter - Missing Authorization header

2025-12-12 20:35:18.012 [http-nio-8080-exec-4] ERROR c.a.m.a.JwtAuthenticationFilter - Invalid JWT signature
```

### Error Log File

All authentication errors are written to `logs/error.log`:

```bash
# Monitor auth failures in real-time
tail -f logs/error.log | grep -i auth
```

### Error Types

**Bearer Mode:**
- Missing Authorization header
- Invalid Bearer token format

**JWT Mode:**
- JWT expired
- Invalid signature
- Missing subject claim
- Missing service tokens (JIRA_TOKEN, CONFLUENCE_TOKEN)

## Log Levels

### Changing Log Levels

#### Environment Variables

```bash
export LOGGING_LEVEL_COM_ATLASSIAN_MCP=DEBUG
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK=WARN
mvn spring-boot:run
```

#### application.yml

```yaml
logging:
  level:
    com.atlassian.mcp: DEBUG
    com.atlassian.mcp.jira: TRACE
    com.atlassian.mcp.auth: INFO
    org.springframework.web: WARN
    reactor.netty: INFO
```

#### Custom log4j2.xml

```xml
<Loggers>
    <Logger name="com.atlassian.mcp" level="DEBUG"/>
    <Logger name="com.atlassian.mcp.jira" level="TRACE"/>
    <Root level="INFO"/>
</Loggers>
```

### Level Hierarchy

```
TRACE < DEBUG < INFO < WARN < ERROR < FATAL
```

### Recommendations

- **Development:** DEBUG or TRACE
- **Staging:** DEBUG or INFO
- **Production:** INFO or WARN

## Advanced Examples

### 1. Kafka Appender

```xml
<!-- Add log4j2-kafka-appender.jar to lib/ -->
<Appenders>
    <Kafka name="Kafka" topic="atlassian-mcp-logs">
        <PatternLayout pattern="%d{ISO8601} %-5level %logger{36} - %msg%n"/>
        <Property name="bootstrap.servers">kafka:9092</Property>
        <Property name="compression.type">gzip</Property>
    </Kafka>
</Appenders>

<Loggers>
    <Root level="INFO">
        <AppenderRef ref="Kafka"/>
    </Root>
</Loggers>
```

### 2. Elasticsearch Appender

```xml
<!-- Add log4j2-elasticsearch-*.jar to lib/ -->
<Appenders>
    <Elasticsearch name="Elastic"
                   serverUris="http://elasticsearch:9200"
                   indexName="mcp-logs-${date:yyyy.MM.dd}">
        <JsonLayout compact="true" eventEol="true"/>
    </Elasticsearch>
</Appenders>
```

### 3. Splunk HTTP Event Collector

```xml
<Appenders>
    <Http name="Splunk"
          url="https://splunk:8088/services/collector"
          method="POST">
        <Property name="Authorization">Splunk YOUR_HEC_TOKEN</Property>
        <JsonLayout complete="false" compact="true"/>
    </Http>
</Appenders>
```

### 4. Async Logging (Performance)

```xml
<Appenders>
    <Async name="AsyncFile" blocking="false">
        <AppenderRef ref="RollingFile"/>
    </Async>
</Appenders>

<Loggers>
    <AsyncLogger name="com.atlassian.mcp" level="INFO">
        <AppenderRef ref="AsyncFile"/>
    </AsyncLogger>
</Loggers>
```

### 5. Structured JSON Logging

```xml
<Appenders>
    <RollingFile name="JsonFile" fileName="logs/mcp.json">
        <JsonTemplateLayout eventTemplateUri="classpath:LogstashJsonEventLayoutV1.json"/>
    </RollingFile>
</Appenders>
```

### 6. Colored Console Output

```xml
<Appenders>
    <Console name="Console" target="SYSTEM_OUT">
        <PatternLayout pattern="%d{ISO8601} %highlight{%-5level} %style{%logger{36}}{cyan} - %msg%n"/>
    </Console>
</Appenders>
```

### 7. Separate Files by Level

```xml
<Appenders>
    <!-- INFO logs -->
    <RollingFile name="InfoFile" fileName="logs/info.log">
        <Filters>
            <ThresholdFilter level="WARN" onMatch="DENY" onMismatch="NEUTRAL"/>
            <ThresholdFilter level="INFO" onMatch="ACCEPT" onMismatch="DENY"/>
        </Filters>
    </RollingFile>
    
    <!-- WARN logs -->
    <RollingFile name="WarnFile" fileName="logs/warn.log">
        <Filters>
            <ThresholdFilter level="ERROR" onMatch="DENY" onMismatch="NEUTRAL"/>
            <ThresholdFilter level="WARN" onMatch="ACCEPT" onMismatch="DENY"/>
        </Filters>
    </RollingFile>
</Appenders>
```

## Troubleshooting

### Problem: No Log Files Created

**Cause:** Directory permissions

**Solution:**
```bash
mkdir -p logs
chmod 755 logs
```

### Problem: Custom Config Not Loaded

**Cause:** Incorrect path or property name

**Solution:**
```bash
# Check absolute path
ls -la /path/to/log4j2.xml

# Use absolute path
export LOGGING_CONFIG=/absolute/path/to/log4j2.xml

# Verify with verbose output
mvn spring-boot:run -X | grep "log4j2"
```

### Problem: External Library Not Found

**Cause:** Library not in lib/ folder or incorrect loader.path

**Solution:**
```bash
# Verify JAR in lib/
ls -la lib/*.jar

# Check pom.xml configuration
grep -A5 "loader.path" pom.xml

# Run with explicit path
java -Dloader.path=lib/ -jar target/*.jar
```

### Problem: JWT Audit Not Logging

**Causes:**
1. Not in JWT mode
2. Log level too high
3. Logger configuration missing

**Solutions:**
```bash
# 1. Enable JWT mode
export SECURITY_MODE=jwt
export JWT_SECRET=your-secret

# 2. Check log level
export LOGGING_LEVEL_COM_ATLASSIAN_MCP_AUTH_JWTAUDITLOGGER=INFO

# 3. Verify configuration
cat src/main/resources/log4j2-spring.xml | grep JwtAuditLogger
```

### Problem: Too Many Log Files

**Cause:** Short retention period or small file sizes

**Solution:**
```xml
<RollingFile>
    <Policies>
        <SizeBasedTriggeringPolicy size="500MB"/>  <!-- Increase size -->
    </Policies>
    <DefaultRolloverStrategy max="90"/>  <!-- Increase retention -->
</RollingFile>
```

### Problem: Logs Not Rotating

**Cause:** Application doesn't run past rollover time/size

**Solution:**
```xml
<!-- Force immediate rollover on startup -->
<RollingFile>
    <Policies>
        <OnStartupTriggeringPolicy/>
        <TimeBasedTriggeringPolicy interval="1"/>
    </Policies>
</RollingFile>
```

### Problem: Performance Impact

**Cause:** Synchronous logging

**Solution:** Use async logging

```xml
<Appenders>
    <Async name="AsyncFile" blocking="false" bufferSize="512">
        <AppenderRef ref="RollingFile"/>
    </Async>
</Appenders>
```

## Configuration Checklist

Before deploying to production:

- [ ] Log rotation configured (daily + size-based)
- [ ] Retention policy set (e.g., 30 days)
- [ ] Error logs separate file
- [ ] JWT audit logging enabled (if using JWT mode)
- [ ] Sensitive data masking verified
- [ ] Log levels appropriate (INFO or WARN)
- [ ] Async logging for performance (optional)
- [ ] External appenders configured (if needed)
- [ ] Disk space monitoring setup
- [ ] Log aggregation configured (Splunk, ELK, etc.)

## Quick Reference

### Start with Default Logging

```bash
mvn spring-boot:run
```

### Start with Custom Config

```bash
export LOGGING_CONFIG=/path/to/log4j2.xml
mvn spring-boot:run
```

### Enable JWT Audit Logging

```bash
export SECURITY_MODE=jwt
export JWT_SECRET=your-secret
mvn spring-boot:run
```

### Change Log Level

```bash
export LOGGING_LEVEL_COM_ATLASSIAN_MCP=DEBUG
mvn spring-boot:run
```

### Monitor Logs

```bash
# All logs
tail -f logs/atlassian-mcp.log

# Errors only
tail -f logs/error.log

# JWT audit only
tail -f logs/jwt-audit.log

# Auth failures
tail -f logs/error.log | grep -i auth
```

---

**See Also:**
- [Authentication Guide](AUTHENTICATION.md)
- [API Reference](API.md)
- [Security Policy](../SECURITY.md)
