package com.ensam.ma.service;

import com.ensam.ma.config.RAGConfig;
import com.ensam.ma.model.Course;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) Service
 * Indexes course content and retrieves relevant chunks for quiz generation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RAGService {
    
    private final RAGConfig ragConfig;
    
    // In-memory vector store (production: use proper vector database)
    private final Map<Long, List<ContentChunk>> courseVectorStore = new HashMap<>();
    
    /**
     * Index course content for RAG retrieval
     */
    public void indexCourse(Course course) {
        log.info("Indexing course: {}", course.getTitle());
        
        String content = course.getContent();
        if (content == null || content.isEmpty()) {
            log.warn("Course {} has no content to index", course.getId());
            return;
        }
        
        List<ContentChunk> chunks = chunkContent(content, course.getId());
        courseVectorStore.put(course.getId(), chunks);
        
        log.info("Indexed {} chunks for course {}", chunks.size(), course.getId());
    }
    
    /**
     * Retrieve relevant content chunks for a query
     */
    public String retrieveContext(Long courseId, String query) {
        log.info("Retrieving context for course {} with query: {}", courseId, query);
        
        List<ContentChunk> chunks = courseVectorStore.get(courseId);
        if (chunks == null || chunks.isEmpty()) {
            log.warn("No indexed content for course {}", courseId);
            return "";
        }
        
        // For quiz generation, we want comprehensive coverage
        // So we'll return all chunks (simulating full retrieval)
        // In production, implement proper similarity scoring
        
        List<ContentChunk> relevantChunks = chunks.stream()
            .limit(ragConfig.getMaxResults())
            .collect(Collectors.toList());
        
        String context = relevantChunks.stream()
            .map(ContentChunk::getText)
            .collect(Collectors.joining("\n\n---\n\n"));
        
        log.info("Retrieved {} chunks totaling {} characters", relevantChunks.size(), context.length());
        
        return context;
    }
    
    /**
     * Retrieve ALL content for comprehensive quiz generation
     */
    public String retrieveFullContext(Long courseId) {
        log.info("Retrieving full context for course {}", courseId);
        
        List<ContentChunk> chunks = courseVectorStore.get(courseId);
        if (chunks == null || chunks.isEmpty()) {
            log.warn("No indexed content for course {}", courseId);
            return "";
        }
        
        String fullContext = chunks.stream()
            .map(ContentChunk::getText)
            .collect(Collectors.joining("\n\n"));
        
        log.info("Retrieved full context: {} chunks, {} characters", chunks.size(), fullContext.length());
        
        return fullContext;
    }
    
    /**
     * Check if a course is indexed
     */
    public boolean isCourseIndexed(Long courseId) {
        return courseVectorStore.containsKey(courseId) && !courseVectorStore.get(courseId).isEmpty();
    }
    
    /**
     * Remove course from index
     */
    public void removeIndex(Long courseId) {
        courseVectorStore.remove(courseId);
        log.info("Removed index for course {}", courseId);
    }
    
    /**
     * Chunk content into overlapping segments
     */
    private List<ContentChunk> chunkContent(String content, Long courseId) {
        List<ContentChunk> chunks = new ArrayList<>();
        
        int chunkSize = ragConfig.getChunkSize();
        int overlap = ragConfig.getChunkOverlap();
        
        // Simple chunking by fixed size with overlap
        int start = 0;
        int chunkNumber = 0;
        
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            
            // Try to break at sentence boundary if possible
            if (end < content.length()) {
                int lastPeriod = content.lastIndexOf('.', end);
                int lastNewline = content.lastIndexOf('\n', end);
                int breakPoint = Math.max(lastPeriod, lastNewline);
                
                if (breakPoint > start + chunkSize / 2) {
                    end = breakPoint + 1;
                }
            }
            
            String chunkText = content.substring(start, end).trim();
            if (!chunkText.isEmpty()) {
                ContentChunk chunk = new ContentChunk();
                chunk.setCourseId(courseId);
                chunk.setChunkNumber(chunkNumber++);
                chunk.setText(chunkText);
                chunk.setStartPos(start);
                chunk.setEndPos(end);
                chunks.add(chunk);
            }
            
            start = end - overlap;
            if (start >= content.length() || (end >= content.length() && start < end)) {
                break;
            }
        }
        
        return chunks;
    }
    
    /**
     * Content chunk data structure
     */
    @lombok.Data
    public static class ContentChunk {
        private Long courseId;
        private int chunkNumber;
        private String text;
        private int startPos;
        private int endPos;
    }
}
