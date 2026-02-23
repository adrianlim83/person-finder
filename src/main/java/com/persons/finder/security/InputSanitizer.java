package com.persons.finder.security;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class InputSanitizer {
    
    private static final int MAX_INPUT_LENGTH = 500;

    private static final List<Pattern> DANGEROUS_PATTERNS = List.of(
        // 1️⃣ Script / XSS related
        Pattern.compile(
                "(?is)" +
                        "<\\s*script\\b[^>]*>.*?<\\s*/\\s*script>|" +       // <script>...</script>
                        "(javascript|vbscript)\\s*:|" +                     // javascript:
                        "data\\s*:\\s*text\\s*/\\s*(html|javascript|css)",  // data:text/...
                Pattern.DOTALL
        ),

        // 2️⃣ Prompt injection – instruction override
        Pattern.compile(
                "(?i)" +
                        "(ignore|disregard)\\s+(all\\s+)?previous\\s+instructions?|"+
                        "forget\\s+(everything|all)\\s+(above|before)|" +
                        "new\\s+instructions?\\s*:|" +
                        "from\\s+now\\s+on|" +
                        "you\\s+are\\s+now|" +
                        "pretend\\s+you\\s+are|" +
                        "act\\s+as"
        ),

        // 3️⃣ Role / system override attempts
        Pattern.compile(
                "(?i)" +
                        "(system|admin)\\s*:|" +
                        "[\\[{<]\\s*system\\s*[\\]}>]"
        )
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
