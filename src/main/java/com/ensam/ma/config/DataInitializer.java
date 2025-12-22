package com.ensam.ma.config;

import com.ensam.ma.model.Course;
import com.ensam.ma.model.Role;
import com.ensam.ma.model.User;
import com.ensam.ma.repository.CourseRepository;
import com.ensam.ma.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Database initialization with sample data
 * Creates test users and courses for demonstration
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) {
        log.info("Initializing database with sample data...");
        
        // Create demo users
        User admin = createAdmin();
        User student1 = createStudent("alice", "Alice Johnson", "alice@example.com");
        User student2 = createStudent("bob", "Bob Smith", "bob@example.com");
        User student3 = createStudent("carol", "Carol Davis", "carol@example.com");
        
        // Create comprehensive courses
        Course springBootCourse = createSpringBootCourse(admin);
        Course aiCourse = createAICourse(admin);
        Course javaCourse = createJavaCourse(admin);
        
        // Enroll students in courses
        enrollStudents(springBootCourse, student1, student2, student3);
        enrollStudents(aiCourse, student1, student2);
        enrollStudents(javaCourse, student2, student3);
        
        log.info("Database initialization complete!");
        log.info("=== Demo Credentials ===");
        log.info("Administrator: admin / admin");
        log.info("Students: alice/alice, bob/bob, carol/carol");
    }
    
    private User createAdmin() {
        if (userRepository.existsByUsername("admin")) {
            return userRepository.findByUsername("admin").orElseThrow();
        }
        
        User admin = User.builder()
            .username("admin")
            .password(passwordEncoder.encode("admin"))
            .fullName("System Administrator")
            .email("admin@pedagogical-platform.com")
            .role(Role.ADMINISTRATOR)
            .enabled(true)
            .build();
        
        return userRepository.save(admin);
    }
    
    private User createStudent(String username, String fullName, String email) {
        if (userRepository.existsByUsername(username)) {
            return userRepository.findByUsername(username).orElseThrow();
        }
        
        User student = User.builder()
            .username(username)
            .password(passwordEncoder.encode(username)) // Same as username for demo
            .fullName(fullName)
            .email(email)
            .role(Role.STUDENT)
            .enabled(true)
            .build();
        
        return userRepository.save(student);
    }
    
    private Course createSpringBootCourse(User creator) {
        String content = """
# Introduction to Spring Boot
            
Spring Boot is a powerful framework built on top of the Spring Framework that simplifies the development of production-ready applications.

## Core Concepts

### 1. Auto-Configuration
Spring Boot's auto-configuration automatically configures Spring applications based on the dependencies present on the classpath. For example, if H2 database is on the classpath, Spring Boot will automatically configure an embedded database. This reduces the need for explicit configuration and accelerates development.

### 2. Spring Boot Starters
Starters are dependency descriptors that include all the dependencies needed for a specific functionality. For example:
- spring-boot-starter-web includes embedded Tomcat, Spring MVC, and JSON libraries
- spring-boot-starter-data-jpa includes Hibernate, Spring Data JPA, and JDBC
- spring-boot-starter-security includes Spring Security and its dependencies

### 3. Embedded Servers
Spring Boot includes embedded servlet containers (Tomcat, Jetty, or Undertow) that allow applications to run as standalone executable JARs. This eliminates the need for deploying WAR files to external servers and simplifies deployment.

### 4. Spring Boot Actuator
Actuator provides production-ready features like health checks, metrics, and monitoring endpoints. These features help in understanding application behavior and diagnosing problems in production.

### 5. Application Properties
Spring Boot uses application.properties or application.yml for centralized configuration. Properties can be overridden using environment variables, command-line arguments, or profile-specific configuration files.

## Security in Spring Boot

Spring Security integration in Spring Boot is streamlined through auto-configuration. Key features include:
- Form-based and HTTP Basic authentication
- Role-based access control
- CSRF protection
- Session management
- Password encoding with BCrypt

Spring Security uses filters to intercept requests and apply security rules before they reach the controller layer.

## Data Persistence with JPA

Spring Boot integrates seamlessly with JPA (Java Persistence API) through Spring Data JPA:
- Entity management with @Entity annotations
- Repository interfaces extending JpaRepository
- Automatic query generation from method names
- Transaction management with @Transactional
- Database migration with Flyway or Liquibase

## Best Practices

1. Use constructor injection instead of field injection
2. Externalize configuration using application.properties
3. Implement proper exception handling with @ControllerAdvice
4. Use DTOs to separate domain models from API contracts
5. Implement comprehensive logging
6. Write integration tests using @SpringBootTest
7. Use profiles for environment-specific configuration
8. Follow RESTful API design principles

## Architecture Patterns

Spring Boot applications typically follow a layered architecture:
- **Controller Layer**: Handles HTTP requests and responses
- **Service Layer**: Contains business logic
- **Repository Layer**: Manages data persistence
- **Domain Model**: Represents business entities

This separation of concerns improves maintainability and testability.
            """;
        
        Course course = Course.builder()
            .title("Introduction to Spring Boot")
            .description("Comprehensive guide to Spring Boot framework, covering auto-configuration, starters, security, and best practices.")
            .content(content)
            .published(true)
            .indexed(false)
            .createdBy(creator)
            .build();
        
        return courseRepository.save(course);
    }
    
    private Course createAICourse(User creator) {
        String content = """
# Artificial Intelligence Fundamentals
            
Artificial Intelligence (AI) is the simulation of human intelligence processes by machines, especially computer systems.

## Machine Learning

Machine Learning (ML) is a subset of AI that enables systems to learn and improve from experience without being explicitly programmed.

### Supervised Learning
In supervised learning, algorithms learn from labeled training data to make predictions on unseen data. Common algorithms include:
- Linear Regression for continuous predictions
- Logistic Regression for binary classification
- Decision Trees for interpretable models
- Random Forests for robust ensemble predictions
- Support Vector Machines for high-dimensional data
- Neural Networks for complex pattern recognition

### Unsupervised Learning
Unsupervised learning finds hidden patterns in unlabeled data:
- K-Means Clustering groups similar data points
- Hierarchical Clustering creates dendrograms
- Principal Component Analysis (PCA) reduces dimensionality
- Autoencoders learn compressed representations

### Reinforcement Learning
RL involves agents learning to make decisions by interacting with an environment to maximize cumulative rewards. Applications include game playing, robotics, and autonomous systems.

## Deep Learning

Deep Learning uses neural networks with multiple layers to learn hierarchical representations of data.

### Neural Network Architectures
- **Feedforward Networks**: Basic architecture for classification and regression
- **Convolutional Neural Networks (CNNs)**: Specialized for image processing and computer vision
- **Recurrent Neural Networks (RNNs)**: Process sequential data like text and time series
- **Long Short-Term Memory (LSTM)**: Address the vanishing gradient problem in RNNs
- **Transformers**: State-of-the-art architecture for NLP tasks

### Training Deep Networks
- Backpropagation algorithm computes gradients
- Optimization algorithms (SGD, Adam, RMSprop) update weights
- Regularization techniques (Dropout, L1/L2) prevent overfitting
- Batch normalization accelerates training
- Learning rate scheduling improves convergence

## Natural Language Processing (NLP)

NLP enables computers to understand, interpret, and generate human language.

### Key Techniques
- Tokenization splits text into words or subwords
- Word embeddings (Word2Vec, GloVe) represent words as vectors
- Named Entity Recognition identifies entities in text
- Sentiment Analysis determines emotional tone
- Machine Translation converts between languages
- Text Summarization condenses documents

### Modern NLP
Transformer-based models have revolutionized NLP:
- BERT: Bidirectional encoder representations
- GPT: Generative pre-trained transformer
- T5: Text-to-text transfer transformer

## Large Language Models (LLMs)

LLMs are neural networks trained on vast amounts of text data to understand and generate human-like text.

### Architecture
LLMs use the transformer architecture with:
- Self-attention mechanisms to capture context
- Massive parameter counts (billions to trillions)
- Pre-training on diverse internet text
- Fine-tuning for specific tasks

### Applications
- Text generation and completion
- Question answering
- Code generation and debugging
- Language translation
- Summarization and paraphrasing
- Conversational AI and chatbots

### Limitations
- Hallucination: generating false information
- Bias from training data
- Lack of true understanding
- Computational requirements
- Context window limitations

## Retrieval-Augmented Generation (RAG)

RAG enhances LLM responses by combining retrieval from external knowledge bases with generation.

### RAG Pipeline
1. **Indexing**: Documents are chunked and embedded into vector representations
2. **Retrieval**: Query embedding finds similar chunks in vector store
3. **Augmentation**: Retrieved context is added to LLM prompt
4. **Generation**: LLM generates response using retrieved context

### Benefits
- Grounds responses in factual information
- Reduces hallucination
- Enables access to private/recent data
- Cost-effective compared to fine-tuning
- Easy to update knowledge base

## Computer Vision

Computer Vision enables machines to derive meaningful information from visual inputs.

### Image Classification
Assigning labels to entire images using CNNs:
- ResNet: Deep residual networks
- EfficientNet: Optimized architectures
- Vision Transformers: Self-attention for images

### Object Detection
Identifying and localizing objects in images:
- YOLO: Real-time detection
- Faster R-CNN: Region-based detection
- SSD: Single-shot multi-box detection

### Image Segmentation
Pixel-level classification:
- Semantic segmentation: Classify each pixel
- Instance segmentation: Identify individual objects
- U-Net: Popular architecture for medical imaging

## AI Ethics and Responsible AI

### Key Principles
1. **Fairness**: Avoid bias and discrimination
2. **Transparency**: Explain AI decisions
3. **Privacy**: Protect user data
4. **Accountability**: Assign responsibility for AI actions
5. **Safety**: Ensure AI systems are robust and secure

### Challenges
- Algorithmic bias from biased training data
- Privacy concerns with data collection
- Explainability of complex models
- Job displacement concerns
- Autonomous systems safety
- Dual-use technology risks

### Best Practices
- Diverse and representative training data
- Regular bias audits
- Privacy-preserving techniques
- Explainable AI (XAI) methods
- Human-in-the-loop systems
- Ethical review processes
            """;
        
        Course course = Course.builder()
            .title("Artificial Intelligence Fundamentals")
            .description("Comprehensive introduction to AI, covering Machine Learning, Deep Learning, NLP, LLMs, RAG, Computer Vision, and AI Ethics.")
            .content(content)
            .published(true)
            .indexed(false)
            .createdBy(creator)
            .build();
        
        return courseRepository.save(course);
    }
    
    private Course createJavaCourse(User creator) {
        String content = """
# Advanced Java Programming
            
Advanced Java programming encompasses sophisticated techniques and patterns for building robust, scalable applications.

## Java Streams API

The Streams API, introduced in Java 8, provides a functional approach to processing collections of data.

### Stream Operations
Streams support two types of operations:

**Intermediate Operations** (lazy evaluation):
- filter(): Select elements based on a predicate
- map(): Transform elements
- flatMap(): Flatten nested structures
- distinct(): Remove duplicates
- sorted(): Sort elements
- limit(): Truncate stream
- skip(): Skip elements

**Terminal Operations** (trigger execution):
- forEach(): Perform action on each element
- collect(): Accumulate elements into collection
- reduce(): Combine elements to single result
- count(): Count elements
- anyMatch()/allMatch()/noneMatch(): Test predicates
- findFirst()/findAny(): Find elements

### Parallel Streams
Parallel streams leverage multi-core processors to process data concurrently:
```
list.parallelStream()
    .filter(predicate)
    .map(function)
    .collect(Collectors.toList());
```

Use parallel streams for CPU-intensive operations on large datasets, but be cautious of thread-safety and overhead.

## Lambda Expressions

Lambda expressions provide a concise way to represent anonymous functions.

### Syntax
```
(parameters) -> expression
(parameters) -> { statements; }
```

### Functional Interfaces
Lambdas work with functional interfaces (interfaces with single abstract method):
- Predicate<T>: Boolean-valued function
- Function<T,R>: Transform input to output
- Consumer<T>: Accept input, return nothing
- Supplier<T>: Provide output with no input
- BiFunction<T,U,R>: Two-parameter function

### Method References
Shorthand notation for lambda expressions:
- Class::staticMethod
- object::instanceMethod
- Class::instanceMethod
- Class::new (constructor reference)

## Java Concurrency

Concurrency enables applications to execute multiple tasks simultaneously.

### Thread Management
- Thread class and Runnable interface
- ExecutorService for thread pool management
- Callable and Future for returning results
- CompletableFuture for asynchronous programming

### Synchronization
- synchronized keyword for method and block synchronization
- volatile keyword for memory visibility
- Locks (ReentrantLock) for explicit locking
- ReadWriteLock for read-write scenarios

### Thread-Safe Collections
- ConcurrentHashMap: Thread-safe hash map
- CopyOnWriteArrayList: Thread-safe list for read-heavy workloads
- BlockingQueue: Producer-consumer pattern
- ConcurrentLinkedQueue: Non-blocking queue

### Fork/Join Framework
Designed for divide-and-conquer algorithms:
- ForkJoinPool: Specialized executor
- RecursiveTask/RecursiveAction: Task decomposition
- Work-stealing algorithm for load balancing

## Design Patterns

Design patterns are reusable solutions to common software design problems.

### Creational Patterns
**Singleton**: Ensures a class has only one instance
- Eager initialization vs lazy initialization
- Thread-safe implementation considerations
- Enum singleton (recommended approach)

**Factory Method**: Creates objects without specifying exact class
- Promotes loose coupling
- Easy to extend with new product types

**Builder**: Constructs complex objects step by step
- Fluent interface design
- Immutable object creation
- Handles optional parameters elegantly

### Structural Patterns
**Adapter**: Converts interface of a class into another interface
- Wraps incompatible interfaces
- Enables integration of legacy code

**Decorator**: Adds behavior to objects dynamically
- Alternative to subclassing
- Follows Open/Closed Principle

**Proxy**: Provides surrogate or placeholder for another object
- Lazy initialization
- Access control
- Remote proxies (RMI, web services)

### Behavioral Patterns
**Strategy**: Defines family of algorithms and makes them interchangeable
- Encapsulates algorithms
- Enables runtime selection

**Observer**: Defines one-to-many dependency between objects
- Event-driven programming
- Java's Observer/Observable (deprecated)
- Modern alternatives: PropertyChangeListener, Java 9 Flow API

**Command**: Encapsulates request as an object
- Undo/redo functionality
- Transaction management
- Request queuing

## Optional and Null Safety

Optional<T> is a container object which may or may not contain a non-null value.

### Best Practices
- Never return null for collections (return empty collection)
- Use Optional for return types that may be absent
- Avoid Optional in fields and parameters
- Use orElse(), orElseGet(), orElseThrow()
- Chain operations with map(), flatMap(), filter()

## Generics

Generics enable types to be parameters when defining classes, interfaces, and methods.

### Type Parameters
- <T> for type
- <E> for element
- <K,V> for key-value pairs
- <? extends T> for upper bounded wildcard
- <? super T> for lower bounded wildcard

### Benefits
- Type safety at compile time
- Eliminates type casting
- Enables generic algorithms

## Performance Optimization

### Memory Management
- Understand heap vs stack memory
- Monitor garbage collection behavior
- Use appropriate data structures
- Avoid memory leaks (listeners, caches)
- Consider object pooling for expensive objects

### Algorithm Optimization
- Choose appropriate data structures (ArrayList vs LinkedList)
- Use lazy initialization when appropriate
- Cache computed results
- Minimize synchronization scope
- Profile before optimizing

### JVM Tuning
- Heap size configuration (-Xms, -Xmx)
- Garbage collector selection (G1GC, ZGC)
- JIT compiler optimization
- Monitoring with JConsole, VisualVM

## Functional Programming Principles

### Immutability
- Immutable objects are thread-safe
- Use final fields
- Don't provide setters
- Make defensive copies

### Pure Functions
- Same input always produces same output
- No side effects
- Easier to test and reason about
- Enables optimization

### Higher-Order Functions
Functions that take functions as parameters or return functions:
- map, filter, reduce operations
- Function composition
- Currying and partial application

## Best Practices

1. **Code Quality**: Write clean, readable, self-documenting code
2. **SOLID Principles**: Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion
3. **Testing**: Unit tests, integration tests, test-driven development
4. **Documentation**: Javadoc for public APIs
5. **Error Handling**: Use exceptions appropriately, don't catch generic Exception
6. **Resource Management**: Use try-with-resources for AutoCloseable
7. **Performance**: Profile before optimizing, prefer simplicity
8. **Security**: Validate input, use parameterized queries, encrypt sensitive data
            """;
        
        Course course = Course.builder()
            .title("Advanced Java Programming")
            .description("Master advanced Java concepts including Streams, Lambda Expressions, Concurrency, Design Patterns, and Performance Optimization.")
            .content(content)
            .published(true)
            .indexed(false)
            .createdBy(creator)
            .build();
        
        return courseRepository.save(course);
    }
    
    private void enrollStudents(Course course, User... students) {
        for (User student : students) {
            course.enrollStudent(student);
        }
        courseRepository.save(course);
        log.info("Enrolled {} students in course: {}", students.length, course.getTitle());
    }
}
