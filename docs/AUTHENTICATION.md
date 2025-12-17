# Authentication Guide

This server supports two authentication modes to accommodate different deployment scenarios.

## Authentication Modes

### 1. Bearer Token Mode (Default)

**Best for:** Development, testing, single-user environments

#### How It Works

1. Client sends Bearer token in Authorization header
2. Server extracts token and uses it for both Jira and Confluence
3. No token validation (relies on Atlassian's API validation)

#### Configuration

```bash
# .env or environment variables
SECURITY_MODE=none  # or omit (default)
```

#### Request Format

```bash
curl -X POST http://localhost:8080/mcp/stream \
  -H "Authorization: Bearer YOUR_ATLASSIAN_API_TOKEN" \
  -H "Content-Type: application/x-ndjson" \
  -d '{"id":"1","type":"REQUEST","method":"jira_get_issue","params":{"issue_key":"PROJ-123"}}'
```

#### Flow Diagram

```
Client Request
     ↓
     ↓ Authorization: Bearer <atlassian_token>
     ↓
BearerTokenFilter
     ↓
     ↓ Sets request attributes:
     ↓   - jira_token = <atlassian_token>
     ↓   - confluence_token = <atlassian_token>
     ↓
AtlassianClientFactory
     ↓
     ↓ Creates clients with token
     ↓
JiraClient / ConfluenceClient
     ↓
     ↓ Calls Atlassian APIs
     ↓
Atlassian APIs (validates token)
```

### 2. JWT Mode (Enterprise)

**Best for:** Production, multi-user environments, enterprise deployments

#### How It Works

1. Client authenticates and receives JWT
2. Client sends JWT + separate service tokens
3. Server validates JWT signature and expiration
4. Server uses service tokens for API calls
5. All requests are audit-logged

#### Configuration

```bash
# .env or environment variables
SECURITY_MODE=jwt
JWT_SECRET=your-secret-key-minimum-32-bytes-for-hs256
```

#### JWT Requirements

**Claims:**
- `sub` (subject): User identifier (required)
- `iat` (issued at): Timestamp (recommended)
- `exp` (expiration): Expiry timestamp (recommended)

**Algorithm:** HS256 (HMAC with SHA-256)

#### Request Format

```bash
curl -X POST http://localhost:8080/mcp/stream \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "JIRA_TOKEN: <jira_api_token>" \
  -H "CONFLUENCE_TOKEN: <confluence_api_token>" \
  -H "Content-Type: application/x-ndjson" \
  -d '{"id":"1","type":"REQUEST","method":"jira_get_issue","params":{"issue_key":"PROJ-123"}}'
```

#### Flow Diagram

```
Client Request
     ↓
     ↓ Authorization: Bearer <JWT>
     ↓ JIRA_TOKEN: <jira_token>
     ↓ CONFLUENCE_TOKEN: <confluence_token>
     ↓
JwtAuthenticationFilter
     ↓
     ↓ 1. Validates JWT signature
     ↓ 2. Checks expiration
     ↓ 3. Extracts subject (user ID)
     ↓ 4. Sets request attributes:
     ↓      - user_id
     ↓      - jira_token
     ↓      - confluence_token
     ↓      - auth_mode = "jwt"
     ↓
JwtAuditLogger (conditional)
     ↓
     ↓ Logs: user, time, tool, params
     ↓
AtlassianClientFactory
     ↓
     ↓ Creates clients with service tokens
     ↓
JiraClient / ConfluenceClient
     ↓
     ↓ Calls Atlassian APIs
     ↓
Atlassian APIs (validates service tokens)
```

## Generating JWT Tokens

### Using Python

```python
import jwt
import time

payload = {
    'sub': 'user@example.com',  # User identifier
    'iat': int(time.time()),    # Issued at
    'exp': int(time.time()) + 86400  # Expires in 24 hours
}

secret = 'your-secret-key-minimum-32-bytes'
token = jwt.encode(payload, secret, algorithm='HS256')

print(token)
```

### Using Java

```java
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtGenerator {
    public static void main(String[] args) {
        String secret = "your-secret-key-minimum-32-bytes-for-hs256";
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        
        String jwt = Jwts.builder()
            .setSubject("user@example.com")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24h
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
        
        System.out.println(jwt);
    }
}
```

### Using Node.js

```javascript
const jwt = require('jsonwebtoken');

const payload = {
  sub: 'user@example.com',
  iat: Math.floor(Date.now() / 1000),
  exp: Math.floor(Date.now() / 1000) + 86400 // 24 hours
};

const secret = 'your-secret-key-minimum-32-bytes';
const token = jwt.sign(payload, secret, { algorithm: 'HS256' });

console.log(token);
```

### Using Online Tool

[jwt.io](https://jwt.io/) - **WARNING:** Never use production secrets on public websites!

## Comparison

| Feature | Bearer Mode | JWT Mode |
|---------|-------------|----------|
| **Setup Complexity** | Simple | Moderate |
| **User Identification** | No | Yes (via JWT sub) |
| **Token Validation** | By Atlassian | By Server + Atlassian |
| **Audit Logging** | No | Yes |
| **Multi-User Support** | Limited | Full |
| **Token Separation** | Single token | JWT + service tokens |
| **Security Level** | Basic | Enterprise |
| **Best For** | Dev/Test | Production |

## Security Best Practices

### JWT Secret

1. **Minimum Length:** 32 bytes (256 bits for HS256)
2. **Randomness:** Use cryptographically secure random generator
3. **Storage:** Store in secrets manager (AWS Secrets Manager, HashiCorp Vault)
4. **Rotation:** Rotate every 90 days

```bash
# Generate secure secret
openssl rand -base64 48
```

### JWT Expiration

```python
# Short-lived tokens (recommended)
payload = {
    'exp': int(time.time()) + 3600  # 1 hour
}

# Long-lived tokens (use with refresh tokens)
payload = {
    'exp': int(time.time()) + 86400  # 24 hours
}
```

### API Token Security

1. **Minimum Permissions:** Use tokens with least privilege
2. **Project-Scoped:** Use project-specific tokens when possible
3. **Rotation:** Rotate every 90 days
4. **Storage:** Never commit tokens to git

```bash
# Bad ❌
JIRA_TOKEN=abc123  # Hardcoded

# Good ✅
JIRA_TOKEN=$(cat /run/secrets/jira_token)  # From secrets manager
```

## Troubleshooting

### "JWT secret is required" Error

**Problem:** Server started in JWT mode without secret

**Solution:**
```bash
export JWT_SECRET=your-secret-key-minimum-32-bytes
mvn spring-boot:run
```

### "JWT validation failed" Error

**Causes:**
1. Invalid signature (wrong secret)
2. Expired token (exp claim passed)
3. Missing subject (no sub claim)
4. Malformed token

**Solution:**
```bash
# Check JWT claims
echo "<JWT>" | cut -d. -f2 | base64 -d | jq

# Verify expiration
date -r $(echo "<JWT>" | cut -d. -f2 | base64 -d | jq -r .exp)
```

### "No Jira token" Error

**Problem:** Missing token in request

**Solution (Bearer mode):**
```bash
curl -H "Authorization: Bearer <token>" ...
```

**Solution (JWT mode):**
```bash
curl -H "Authorization: Bearer <JWT>" \
     -H "JIRA_TOKEN: <jira_token>" ...
```

### 401 Unauthorized

**Bearer Mode:**
- Check token is valid in Atlassian
- Verify token has required permissions

**JWT Mode:**
- Verify JWT signature with correct secret
- Check JWT hasn't expired
- Ensure JWT has `sub` claim
- Verify service tokens are valid

## Environment Variables Reference

```bash
# Authentication Mode
SECURITY_MODE=none|jwt  # Default: none

# JWT Configuration (JWT mode only)
JWT_SECRET=<secret>     # Required for JWT mode

# Service URLs (both modes)
JIRA_BASE_URL=https://your-domain.atlassian.net
CONFLUENCE_BASE_URL=https://your-domain.atlassian.net/wiki
```

## Architecture Details

### Conditional Bean Registration

```java
// Bearer mode filter (default)
@ConditionalOnProperty(
    name = "app.security.mode",
    havingValue = "none",
    matchIfMissing = true  // Active when property not set
)
public class BearerTokenFilter { ... }

// JWT mode filter
@ConditionalOnProperty(
    name = "app.security.mode",
    havingValue = "jwt"
)
public class JwtAuthenticationFilter { ... }
```

**Result:** Only one filter is active at a time (mutually exclusive)

### Request Attributes

Both filters set these attributes:

```java
request.setAttribute("jira_token", token);
request.setAttribute("confluence_token", token);
request.setAttribute("auth_mode", "none" | "jwt");

// JWT mode additionally sets:
request.setAttribute("user_id", userId);
```

### Client Factory

```java
public JiraClient createJiraClient(HttpServletRequest request) {
    String token = (String) request.getAttribute("jira_token");
    
    if (token == null) {
        String mode = (String) request.getAttribute("auth_mode");
        if ("jwt".equals(mode)) {
            throw new RuntimeException("JIRA_TOKEN header required");
        } else {
            throw new RuntimeException("Authorization Bearer token required");
        }
    }
    
    return new JiraClient(jiraBaseUrl, token, objectMapper);
}
```

## Migration Guide

### From Bearer to JWT Mode

1. **Setup JWT Infrastructure:**
   ```bash
   # Generate secret
   export JWT_SECRET=$(openssl rand -base64 48)
   ```

2. **Update Server Configuration:**
   ```bash
   SECURITY_MODE=jwt
   JWT_SECRET=<generated-secret>
   ```

3. **Update Clients:**
   ```python
   # Old (Bearer mode)
   headers = {
       "Authorization": f"Bearer {atlassian_token}"
   }
   
   # New (JWT mode)
   headers = {
       "Authorization": f"Bearer {jwt_token}",
       "JIRA_TOKEN": jira_token,
       "CONFLUENCE_TOKEN": confluence_token
   }
   ```

4. **Test Authentication:**
   ```bash
   # Generate test JWT
   python -c "import jwt; print(jwt.encode({'sub':'test'}, 'secret'))"
   
   # Test request
   curl -H "Authorization: Bearer <JWT>" \
        -H "JIRA_TOKEN: <token>" \
        ...
   ```

5. **Monitor Audit Logs:**
   ```bash
   tail -f logs/jwt-audit.log
   ```

---

**See Also:**
- [Logging Guide](LOGGING.md)
- [Security Policy](../SECURITY.md)
- [API Reference](API.md)