package com.persons.finder.security;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class InputSanitizer {
    
    private static final int MAX_INPUT_LENGTH = 500;
    
    private static final List<Pattern> DANGEROUS_PATTERNS = List.of(
        Pattern.compile("ignore\\s+previous\\s+instructions?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("disregard\\s+all\\s+previous\\s+instructions?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("forget\\s+everything\\s+above", Pattern.CASE_INSENSITIVE),
        Pattern.compile("new\\s+instructions?:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("system\\s*:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("admin\\s*:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\[\\s*system\\s*\\]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\{\\s*system\\s*\\}", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<\\s*system\\s*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("you\\s+are\\s+now", Pattern.CASE_INSENSITIVE),
        Pattern.compile("pretend\\s+you\\s+are", Pattern.CASE_INSENSITIVE),
        Pattern.compile("from\\s+now\\s+on", Pattern.CASE_INSENSITIVE),
        Pattern.compile("act\\s+as", Pattern.CASE_INSENSITIVE)
    );
    
    public String sanitize(String input) {
        if (input == null) {
            return "";
        }
        
        String sanitized = input.trim();
        
        if (sanitized.length() > MAX_INPUT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_INPUT_LENGTH);
        }
        
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("[REDACTED]");
        }
        
        sanitized = sanitized.replaceAll("[\\x00-\\x1F\\x7F]", "");
        
        return sanitized;
    }
    
    public List<String> sanitizeList(List<String> inputs) {
        if (inputs == null) {
            return List.of();
        }
        
        return inputs.stream()
            .map(this::sanitize)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
    
    public boolean containsDangerousPattern(String input) {
        if (input == null) {
            return false;
        }
        
        return DANGEROUS_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(input).find());
    }
}
