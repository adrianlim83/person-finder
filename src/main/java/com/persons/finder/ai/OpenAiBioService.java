package com.persons.finder.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAiBioService implements AiBioService {
    
    @Value("${ai.openai.api-key}")
    private String apiKey;
    
    @Value("${ai.openai.model:gpt-3.5-turbo}")
    private String model;
    
    @Value("${ai.openai.max-tokens:100}")
    private int maxTokens;

    @Value("${ai.openai.temperature:0.7}")
    private double temperature;

    @Value("${ai.openai.timeout-in-seconds:60}")
    private int timeoutInSeconds;
    
    private final RestTemplate restTemplate;
    
    public OpenAiBioService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Override
    public String generateBio(String jobTitle, List<String> hobbies) {
        String prompt = String.format(
            "Write a quirky one-sentence bio for someone who is a %s and enjoys %s.",
            jobTitle,
            String.join(", ", hobbies)
        );
        
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .topP(1.0)
                .timeout(Duration.of(timeoutInSeconds, ChronoUnit.SECONDS))
                .build();
        return chatModel.chat(prompt);
    }
}
