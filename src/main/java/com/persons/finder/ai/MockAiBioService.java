package com.persons.finder.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiBioService implements AiBioService {
    
    private static final List<String> QUIRKY_PREFIXES = List.of(
        "Meet",
        "Behold",
        "Introducing",
        "Say hello to",
        "Here comes"
    );
    
    private static final List<String> QUIRKY_CONNECTORS = List.of(
        "who moonlights as",
        "with a passion for",
        "enthusiastically pursuing",
        "obsessed with",
        "secretly devoted to"
    );
    
    private static final List<String> QUIRKY_ENDINGS = List.of(
        "when not saving the world!",
        "in their spare time!",
        "like there's no tomorrow!",
        "with unwavering dedication!",
        "because why not?"
    );
    
    @Override
    public String generateBio(String jobTitle, List<String> hobbies) {
        if (jobTitle == null || hobbies == null || hobbies.isEmpty()) {
            return "A mysterious individual with untold talents.";
        }
        
        int hash = Math.abs((jobTitle + String.join("", hobbies)).hashCode());
        
        String prefix = QUIRKY_PREFIXES.get(hash % QUIRKY_PREFIXES.size());
        String connector = QUIRKY_CONNECTORS.get((hash / 10) % QUIRKY_CONNECTORS.size());
        String ending = QUIRKY_ENDINGS.get((hash / 100) % QUIRKY_ENDINGS.size());
        
        String hobbyList = hobbies.size() == 1 
            ? hobbies.get(0)
            : String.join(" and ", hobbies.subList(0, Math.min(2, hobbies.size())));
        
        return String.format("%s a %s %s %s %s", 
            prefix, jobTitle, connector, hobbyList, ending);
    }
}
