package com.ensam.ma.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG configuration properties
 */
@Configuration
@ConfigurationProperties(prefix = "rag")
@Getter
@Setter
public class RAGConfig {
    
    private int chunkSize = 500;
    private int chunkOverlap = 100;
    private int maxResults = 5;
}
