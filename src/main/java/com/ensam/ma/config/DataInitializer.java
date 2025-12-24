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
import org.springframework.transaction.annotation.Transactional;

/**
 * Database initialization with sample data
 * Creates test users and courses for demonstration
 * Uses @Transactional to handle lazy loading properly
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            log.info("Initializing database with sample data...");

            // Check if data already exists (for file-based persistence)
            if (courseRepository.count() > 0) {
                log.info("Database already initialized, skipping...");
                log.info("=== Demo Credentials ===");
                log.info("Administrator: admin / admin");
                log.info("Students: alice/alice, bob/bob, carol/carol");
                return;
            }

            // Create demo users
            User admin = createAdmin();
            User student1 = createStudent("alice", "Alice Johnson", "alice@example.com");
            User student2 = createStudent("bob", "Bob Smith", "bob@example.com");
            User student3 = createStudent("carol", "Carol Davis", "carol@example.com");

            // Create comprehensive courses
            Course springBootCourse = createSpringBootCourse(admin);
            Course aiCourse = createAICourse(admin);
            Course javaCourse = createJavaCourse(admin);

            // Enroll students in courses using direct SQL-like approach
            enrollStudentInCourse(springBootCourse, student1);
            enrollStudentInCourse(springBootCourse, student2);
            enrollStudentInCourse(springBootCourse, student3);

            enrollStudentInCourse(aiCourse, student1);
            enrollStudentInCourse(aiCourse, student2);

            enrollStudentInCourse(javaCourse, student2);
            enrollStudentInCourse(javaCourse, student3);

            log.info("Database initialization complete!");
            log.info("=== Demo Credentials ===");
            log.info("Administrator: admin / admin");
            log.info("Students: alice/alice, bob/bob, carol/carol");
        } catch (Exception e) {
            log.error("Error during database initialization", e);
            // Don't throw - allow app to start even if initialization fails
            log.warn("Application will continue, but demo data may not be available");
        }
    }

    private void enrollStudentInCourse(Course course, User student) {
        // Reload entities within transaction to avoid lazy loading issues
        Course freshCourse = courseRepository.findById(course.getId()).orElseThrow();
        User freshStudent = userRepository.findById(student.getId()).orElseThrow();

        // Use the Set directly
        freshCourse.getEnrolledStudents().add(freshStudent);
        courseRepository.save(freshCourse);
        log.info("Enrolled {} in course: {}", freshStudent.getUsername(), freshCourse.getTitle());
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
                            """;

        Course course = Course.builder()
                .title("Introduction to Spring Boot")
                .description(
                        "Comprehensive guide to Spring Boot framework, covering auto-configuration, starters, security, and best practices.")
                .content(content)
                .published(true)
                .indexed(true)
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
                RL involves agents learning to make decisions by interacting with an environment to maximize cumulative rewards.

                ## Deep Learning

                Deep Learning uses neural networks with multiple layers to learn hierarchical representations of data.

                ### Neural Network Architectures
                - Feedforward Networks: Basic architecture for classification and regression
                - Convolutional Neural Networks (CNNs): Specialized for image processing
                - Recurrent Neural Networks (RNNs): Process sequential data like text
                - Long Short-Term Memory (LSTM): Address the vanishing gradient problem
                - Transformers: State-of-the-art architecture for NLP tasks

                ## Natural Language Processing (NLP)

                NLP enables computers to understand, interpret, and generate human language.

                ### Key Techniques
                - Tokenization splits text into words or subwords
                - Word embeddings (Word2Vec, GloVe) represent words as vectors
                - Named Entity Recognition identifies entities in text
                - Sentiment Analysis determines emotional tone

                ## Large Language Models (LLMs)

                LLMs are neural networks trained on vast amounts of text data to understand and generate human-like text.

                ### Architecture
                LLMs use the transformer architecture with:
                - Self-attention mechanisms to capture context
                - Massive parameter counts (billions to trillions)
                - Pre-training on diverse internet text
                - Fine-tuning for specific tasks

                ## Retrieval-Augmented Generation (RAG)

                RAG enhances LLM responses by combining retrieval from external knowledge bases with generation.

                ### RAG Pipeline
                1. Indexing: Documents are chunked and embedded into vector representations
                2. Retrieval: Query embedding finds similar chunks in vector store
                3. Augmentation: Retrieved context is added to LLM prompt
                4. Generation: LLM generates response using retrieved context

                ### Benefits
                - Grounds responses in factual information
                - Reduces hallucination
                - Enables access to private/recent data
                            """;

        Course course = Course.builder()
                .title("Artificial Intelligence Fundamentals")
                .description(
                        "Comprehensive introduction to AI, covering Machine Learning, Deep Learning, NLP, LLMs, and RAG.")
                .content(content)
                .published(true)
                .indexed(true)
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

                **Terminal Operations** (trigger execution):
                - forEach(): Perform action on each element
                - collect(): Accumulate elements into collection
                - reduce(): Combine elements to single result
                - count(): Count elements
                - findFirst()/findAny(): Find elements

                ## Lambda Expressions

                Lambda expressions provide a concise way to represent anonymous functions.

                ### Functional Interfaces
                Lambdas work with functional interfaces (interfaces with single abstract method):
                - Predicate<T>: Boolean-valued function
                - Function<T,R>: Transform input to output
                - Consumer<T>: Accept input, return nothing
                - Supplier<T>: Provide output with no input

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

                ## Design Patterns

                Design patterns are reusable solutions to common software design problems.

                ### Creational Patterns
                **Singleton**: Ensures a class has only one instance
                **Factory Method**: Creates objects without specifying exact class
                **Builder**: Constructs complex objects step by step

                ### Structural Patterns
                **Adapter**: Converts interface of a class into another interface
                **Decorator**: Adds behavior to objects dynamically
                **Proxy**: Provides surrogate or placeholder for another object

                ### Behavioral Patterns
                **Strategy**: Defines family of algorithms and makes them interchangeable
                **Observer**: Defines one-to-many dependency between objects
                **Command**: Encapsulates request as an object

                ## Best Practices

                1. Write clean, readable, self-documenting code
                2. Follow SOLID Principles
                3. Use unit tests and test-driven development
                4. Document public APIs with Javadoc
                5. Use exceptions appropriately
                6. Use try-with-resources for AutoCloseable
                            """;

        Course course = Course.builder()
                .title("Advanced Java Programming")
                .description(
                        "Master advanced Java concepts including Streams, Lambda Expressions, Concurrency, and Design Patterns.")
                .content(content)
                .published(true)
                .indexed(true)
                .createdBy(creator)
                .build();

        return courseRepository.save(course);
    }
}
