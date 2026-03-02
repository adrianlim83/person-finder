package com.persons.finder.ai;

import com.persons.finder.config.OpenAIConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
@RequiredArgsConstructor
public class OpenAiBioService implements AiBioService {
    private final OpenAIConfig openAIConfig;

    @Override
    public String generateBio(String jobTitle, List<String> hobbies) {
        // BIO prompt guidance:
        // Write a short friendly bio for a Senior Java Developer who loves hiking, photography, and cooking. Avoid any unnecessary text.
        // Suggested models:
        // - gpt-3.5-turbo: Cost-effective, generally works well, but may be less polished and expressive.
        // - gpt-4.1: Balanced choice, offering strong creativity while maintaining factual accuracy.
        String prompt = String.format(
            openAIConfig.getPrompt(),
            jobTitle,
            String.join(", ", hobbies)
        );
        
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(openAIConfig.getApiKey())
                .modelName(openAIConfig.getModel())
                .maxTokens(openAIConfig.getMaxTokens())
                .temperature(openAIConfig.getTemperature())
                .topP(1.0)
                .timeout(Duration.of(openAIConfig.getTimeoutInSeconds(), ChronoUnit.SECONDS))
                .build();
        return chatModel.chat(prompt);
    }
}
