package com.atlassian.mcp.confluence;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Confluence Query Language (CQL) 유틸리티.
 * Python의 confluence/utils.py의 quote_cql_identifier_if_needed와 동일한 기능 제공.
 */
public class CqlUtils {
    
    /**
     * CQL 예약어 목록 (소문자).
     * https://developer.atlassian.com/cloud/confluence/cql-functions/#reserved-words
     */
    private static final Set<String> RESERVED_CQL_WORDS = Set.of(
        "after", "and", "as", "avg", "before", "begin", "by", "commit", 
        "contains", "count", "distinct", "else", "empty", "end", "explain", 
        "from", "having", "if", "in", "inner", "insert", "into", "is", 
        "isnull", "left", "like", "limit", "max", "min", "not", "null", 
        "or", "order", "outer", "right", "select", "sum", "then", "was", 
        "where", "update"
    );
    
    /**
     * CQL 식별자(identifier)를 필요시 quote 처리합니다.
     * 
     * Quote가 필요한 경우:
     * 1. 틸드(~)로 시작하는 경우 (Personal Space Key)
     * 2. CQL 예약어인 경우
     * 3. 숫자로 시작하는 경우
     * 4. 따옴표(")나 백슬래시(\)를 포함하는 경우
     * 
     * @param identifier 식별자 (예: space key)
     * @return quote 처리된 식별자 또는 원본
     */
    public static String quoteCqlIdentifierIfNeeded(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return identifier;
        }
        
        boolean needsQuoting = false;
        String identifierLower = identifier.toLowerCase();
        
        // Rule 1: ~ 로 시작 (Personal Space Key)
        if (identifier.startsWith("~")) {
            needsQuoting = true;
        }
        // Rule 2: CQL 예약어
        else if (RESERVED_CQL_WORDS.contains(identifierLower)) {
            needsQuoting = true;
        }
        // Rule 3: 숫자로 시작
        else if (Character.isDigit(identifier.charAt(0))) {
            needsQuoting = true;
        }
        // Rule 4: 따옴표나 백슬래시 포함
        else if (identifier.contains("\"") || identifier.contains("\\")) {
            needsQuoting = true;
        }
        
        if (needsQuoting) {
            // 백슬래시 먼저 escape, 그 다음 따옴표 escape
            String escaped = identifier.replace("\\", "\\\\").replace("\"", "\\\"");
            return "\"" + escaped + "\"";
        }
        
        return identifier;
    }
    
    /**
     * CQL 쿼리에서 space key를 자동으로 quote 처리합니다.
     * "space = ~SPACE_KEY" -> "space = \"~SPACE_KEY\""
     * "space=~SPACE_KEY" -> "space=\"~SPACE_KEY\""
     * 
     * @param cql 원본 CQL 쿼리
     * @return quote 처리된 CQL 쿼리
     */
    public static String autoQuoteSpaceKeys(String cql) {
        if (cql == null || cql.isEmpty()) {
            return cql;
        }
        
        // space = 또는 space= 패턴을 찾아서 값 부분을 quote 처리
        // 정규식: space\s*=\s*([^\s"()]+) - space key가 이미 quote되지 않은 경우만
        Pattern pattern = Pattern.compile("space\\s*=\\s*([^\\s\"()]+)");
        Matcher matcher = pattern.matcher(cql);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String spaceKey = matcher.group(1);
            String quotedKey = quoteCqlIdentifierIfNeeded(spaceKey);
            // space = <quoted_key> 형태로 replacement 생성
            String replacement = matcher.group(0).replace(spaceKey, quotedKey);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
}
