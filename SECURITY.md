# Security Policy

## Supported Versions

We release patches for security vulnerabilities for the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

If you discover a security vulnerability, please send an email to [security@example.com](mailto:security@example.com) with:

1. **Description** of the vulnerability
2. **Steps to reproduce** the issue
3. **Potential impact** of the vulnerability
4. **Suggested fix** (if you have one)

You should receive a response within 48 hours. If the issue is confirmed, we will:

1. Work on a fix
2. Release a security advisory
3. Credit you for the discovery (if desired)

## Security Best Practices

### Authentication

This server supports two authentication modes:

#### 1. Bearer Token Mode (Development)

- **Use for**: Development, testing, single-user environments
- **Security Level**: Basic
- **Configuration**:
  ```bash
  SECURITY_MODE=none
  ```

#### 2. JWT Mode (Production)

- **Use for**: Production, multi-user environments
- **Security Level**: Enterprise-grade
- **Configuration**:
  ```bash
  SECURITY_MODE=jwt
  JWT_SECRET=<strong-secret-key-minimum-32-bytes>
  ```

### JWT Security Recommendations

1. **Secret Key**: Use a strong, random secret key (minimum 32 bytes)
   ```bash
   # Generate a secure key
   openssl rand -base64 48
   ```

2. **Key Rotation**: Rotate JWT secrets regularly (e.g., every 90 days)

3. **Expiration**: Set appropriate JWT expiration times
   ```json
   {
     "exp": 1234567890,  # Unix timestamp
     "iat": 1234564290,  # Issued at
     "sub": "user123"    # Subject (user ID)
   }
   ```

4. **HTTPS Only**: Always use HTTPS in production
   ```yaml
   server:
     ssl:
       enabled: true
       key-store: classpath:keystore.p12
       key-store-password: ${KEYSTORE_PASSWORD}
   ```

### Environment Variables

**Never commit sensitive data to version control.**

Use environment variables or secure vaults:

```bash
# Bad ❌
JIRA_TOKEN=abc123secrettoken  # In git

# Good ✅
export JIRA_TOKEN=$(cat /run/secrets/jira_token)  # From secrets management
```

### API Token Security

1. **Minimum Permissions**: Use API tokens with minimal required permissions
2. **Regular Rotation**: Rotate tokens every 90 days
3. **Scope Limitation**: Use project-specific tokens when possible
4. **Audit Logging**: Enable audit logging in JWT mode

### Network Security

1. **Firewall Rules**: Restrict access to port 8080
   ```bash
   # Allow only from specific IPs
   ufw allow from 192.168.1.0/24 to any port 8080
   ```

2. **Reverse Proxy**: Use a reverse proxy (nginx, Apache) with SSL
   ```nginx
   server {
       listen 443 ssl;
       server_name mcp.example.com;
       
       ssl_certificate /path/to/cert.pem;
       ssl_certificate_key /path/to/key.pem;
       
       location / {
           proxy_pass http://localhost:8080;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }
   }
   ```

3. **Rate Limiting**: Implement rate limiting
   ```yaml
   spring:
     cloud:
       gateway:
         routes:
           - id: mcp
             filters:
               - name: RequestRateLimiter
                 args:
                   redis-rate-limiter.replenishRate: 10
                   redis-rate-limiter.burstCapacity: 20
   ```

### Logging Security

1. **Sensitive Data**: Never log sensitive information
   - Passwords
   - API tokens (only last 8 chars)
   - JWT secrets
   - User credentials

2. **Audit Logs**: Enable audit logging in JWT mode
   ```bash
   SECURITY_MODE=jwt
   # Audit logs written to logs/jwt-audit.log
   ```

3. **Log Retention**: Configure appropriate retention
   ```xml
   <!-- log4j2-spring.xml -->
   <DefaultRolloverStrategy max="30"/>  <!-- 30 days -->
   ```

### Dependency Security

1. **Keep Dependencies Updated**: Regularly update dependencies
   ```bash
   mvn versions:display-dependency-updates
   ```

2. **Security Scanning**: Use dependency check plugins
   ```xml
   <plugin>
       <groupId>org.owasp</groupId>
       <artifactId>dependency-check-maven</artifactId>
       <version>8.4.0</version>
   </plugin>
   ```

3. **Vulnerability Alerts**: Enable GitHub Dependabot

### Docker Security

1. **Non-Root User**: Run container as non-root
   ```dockerfile
   FROM eclipse-temurin:21-jre-alpine
   
   RUN addgroup -g 1001 appuser && \
       adduser -D -u 1001 -G appuser appuser
   
   USER appuser
   ```

2. **Minimal Base Image**: Use minimal base images
   ```dockerfile
   FROM eclipse-temurin:21-jre-alpine  # ✅ Small, secure
   # Not: eclipse-temurin:21-jdk  # ❌ Larger attack surface
   ```

3. **Security Scanning**: Scan images regularly
   ```bash
   docker scan mcp-atlassian-java:latest
   ```

### Kubernetes Security

1. **RBAC**: Use Role-Based Access Control
   ```yaml
   apiVersion: rbac.authorization.k8s.io/v1
   kind: Role
   metadata:
     name: mcp-reader
   rules:
     - apiGroups: [""]
       resources: ["pods"]
       verbs: ["get", "list"]
   ```

2. **Network Policies**: Restrict pod communication
   ```yaml
   apiVersion: networking.k8s.io/v1
   kind: NetworkPolicy
   metadata:
     name: mcp-policy
   spec:
     podSelector:
       matchLabels:
         app: mcp-atlassian
     policyTypes:
       - Ingress
     ingress:
       - from:
           - namespaceSelector:
               matchLabels:
                 name: allowed-namespace
   ```

3. **Secrets Management**: Use Kubernetes secrets or external vaults
   ```yaml
   apiVersion: v1
   kind: Secret
   metadata:
     name: mcp-secrets
   type: Opaque
   data:
     jwt-secret: <base64-encoded-secret>
   ```

### Common Vulnerabilities

#### 1. Injection Attacks

**Protection**: Validate all inputs

```java
public Mono<Map<String, Object>> search(Map<String, Object> params) {
    String jql = (String) params.get("jql");
    
    // Validate JQL
    if (jql != null && jql.contains(";")) {
        return Mono.just(Map.of(
            "success", false,
            "error", "Invalid JQL syntax"
        ));
    }
    
    // Proceed with sanitized input
}
```

#### 2. Authentication Bypass

**Protection**: Always validate JWT

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        String token = extractToken(request);
        
        if (!jwtService.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;  // Block request
        }
        
        filterChain.doFilter(request, response);
    }
}
```

#### 3. Information Disclosure

**Protection**: Generic error messages

```java
// Bad ❌
return Map.of("error", "User john@example.com not found in database");

// Good ✅
return Map.of("error", "Invalid credentials");
```

## Security Checklist

Before deploying to production:

- [ ] `SECURITY_MODE=jwt` enabled
- [ ] Strong JWT secret (32+ bytes)
- [ ] HTTPS configured
- [ ] API tokens rotated
- [ ] Firewall rules configured
- [ ] Rate limiting enabled
- [ ] Audit logging enabled
- [ ] Dependencies updated
- [ ] Security scanning performed
- [ ] Non-root Docker user
- [ ] Kubernetes RBAC configured
- [ ] Secrets in vault (not environment)
- [ ] Monitoring and alerting setup

## Security Updates

Subscribe to security updates:

1. Watch this repository for releases
2. Enable GitHub security advisories
3. Subscribe to [security mailing list](mailto:security-announce@example.com)

## Compliance

This project implements security best practices for:

- **OWASP Top 10**: Protection against common vulnerabilities
- **SOC 2**: Audit logging and access controls
- **GDPR**: No PII logging, data minimization

## Contact

For security concerns:
- Email: [security@example.com](mailto:security@example.com)
- PGP Key: [Download](https://example.com/security-pgp.asc)

---

**Last Updated**: December 2025
