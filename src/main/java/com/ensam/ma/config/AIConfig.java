package com.ensam.ma.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI configuration properties for Google Gemini
 */
@Configuration
@ConfigurationProperties(prefix = "gemini")
@Getter
@Setter
public class AIConfig {
    
    private Api api = new Api();
    private String model = "gemini-1.5-flash";
    private double temperature = 0.7;
    private int maxTokens = 2000;
    
    @Getter
    @Setter
    public static class Api {
        private String key;
    }
}
