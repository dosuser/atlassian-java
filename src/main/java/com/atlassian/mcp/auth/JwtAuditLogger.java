package com.atlassian.mcp.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * JWT 감사 로거.
 * JWT 모드에서만 사용되는 전용 로거.
 * 
 * 로그 내용:
 * - JWT subject (사용자 ID)
 * - 호출 시간
 * - MCP 도구명
 * - 파라미터
 */
@Component
public class JwtAuditLogger {
    
    private static final Logger log = LoggerFactory.getLogger(JwtAuditLogger.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * JWT 모드에서 MCP 도구 호출 로그 기록.
     * 
     * @param userId JWT subject
     * @param method MCP 도구명
     * @param params 도구 파라미터
     */
    public void logToolInvocation(String userId, String method, Object params) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        
        // INFO 레벨로 로그 기록
        log.info("JWT_AUDIT | user={} | time={} | tool={} | params={}", 
                userId, timestamp, method, sanitizeParams(params));
    }
    
    /**
     * 파라미터 민감 정보 제거.
     * 비밀번호, 토큰 등의 민감한 정보는 마스킹 처리.
     */
    private String sanitizeParams(Object params) {
        if (params == null) {
            return "{}";
        }
        
        String paramsStr = params.toString();
        
        // 민감 정보 마스킹 (필요시 추가)
        paramsStr = paramsStr.replaceAll("(password|token|secret|key)=[^,}\\]]+", "$1=***");
        
        // 너무 긴 경우 자르기 (최대 500자)
        if (paramsStr.length() > 500) {
            paramsStr = paramsStr.substring(0, 497) + "...";
        }
        
        return paramsStr;
    }
}
