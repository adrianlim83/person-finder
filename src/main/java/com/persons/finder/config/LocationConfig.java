package com.persons.finder.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class LocationConfig {
    @Value("${location.nearby.limit:1000}")
    private int nearbyLimit;
}
