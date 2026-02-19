package com.persons.finder.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class OpenAIConfig {
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
}
