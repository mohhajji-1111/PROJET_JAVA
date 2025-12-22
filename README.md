# Intelligent Pedagogical Platform

> A production-grade educational platform integrating Spring Boot with LLM, RAG, and Agentic AI

## 🎯 Overview

An intelligent, secure educational platform that combines classical Spring Boot architecture with cutting-edge AI:
- **Large Language Models (LLM)** for quiz generation
- **Retrieval-Augmented Generation (RAG)** for content-based questions
- **Agentic AI** for intelligent supervision and pedagogical decisions

## ✨ Features

### For Administrators
- 📚 Create, publish, and manage courses
- 👥 Manage student accounts
- 🔗 Assign students to courses
- 🤖 Trigger RAG indexing for AI quiz generation
- 📊 View platform statistics and usage

### For Students
- 📖 Access assigned courses
- 🎓 Read course content
- 🧠 Generate AI-powered quizzes
- ✅ Take personalized assessments
- 📈 View quiz history and results
- 🏆 Track course validation progress

### AI Intelligence
- **Adaptive Difficulty**: AI adjusts quiz difficulty based on student performance
- **RAG-Based Content**: Questions derived exclusively from course material
- **Intelligent Evaluation**: AI agent makes pedagogical pass/fail decisions
- **Personalized Feedback**: Contextual feedback based on performance and difficulty

## 🛠️ Technology Stack

- **Spring Boot 4.0.1** - Application framework
- **Spring Security** - Authentication & authorization
- **Spring Data JPA** - Data persistence
- **Thymeleaf** - Server-side templating
- **H2 Database** - In-memory database
- **LangChain4j** - LLM integration
- **OpenAI GPT-4** - Large language model
- **Custom RAG** - Content retrieval system
- **Maven** - Build tool

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- OpenAI API key (optional, has fallback)

### Installation

1. **Clone the repository**
```bash
cd PROJET_JAVA
```

2. **Configure OpenAI API Key** (optional)

Set environment variable:
```bash
# Windows
set OPENAI_API_KEY=your-api-key-here

# Linux/Mac
export OPENAI_API_KEY=your-api-key-here
```

Or edit `src/main/resources/application.properties`:
```properties
openai.api.key=your-api-key-here
```

3. **Build and run**
```bash
mvnw clean install
mvnw spring-boot:run
```

4. **Access the application**
```
Application: http://localhost:8080
H2 Console: http://localhost:8080/h2-console
```

## 🔐 Demo Credentials

### Administrator
- Username: `admin`
- Password: `admin123`

### Students
- Username: `john.doe` | Password: `password`
- Username: `jane.smith` | Password: `password`
- Username: `bob.wilson` | Password: `password`

## 📖 User Guide

### Administrator Workflow

1. **Login** with admin credentials
2. **Create a course**:
   - Navigate to Courses > Create New Course
   - Enter title, description, and comprehensive content
   - Save as draft
3. **Publish the course** to make it visible to students
4. **Index the course** to enable RAG-based quiz generation
5. **Create student accounts** via Students > Add New Student
6. **Enroll students** in courses from course details page
7. **Monitor progress** via dashboard statistics

### Student Workflow

1. **Login** with student credentials
2. **View enrolled courses** on dashboard
3. **Read course content** by clicking on a course
4. **Generate AI quiz**:
   - Click "Generate AI Quiz" button
   - AI agent analyzes your history and determines difficulty
   - Personalized quiz is created from course content
5. **Take the quiz**:
   - Answer all questions
   - Submit for AI evaluation
6. **View results**:
   - See your score and AI agent's decision
   - Review correct answers with explanations
   - Read personalized feedback
7. **Retake if needed**:
   - AI adapts difficulty based on your performance
   - Track your progress over multiple attempts

## 🤖 AI Architecture

### Agentic AI Flow

```
Student Request
    ↓
Agent Analyzes History → Determines Difficulty
    ↓
RAG Retrieves Relevant Content
    ↓
LLM Generates Quiz Questions
    ↓
Agent Validates Quiz
    ↓
Student Takes Quiz
    ↓
Agent Evaluates & Decides (Pass/Fail)
    ↓
Provides Personalized Feedback
```

### Key AI Components

1. **RAG Service**: Chunks course content, creates embeddings, retrieves relevant context
2. **LLM Service**: Generates quiz questions via OpenAI API with strict prompts
3. **Agentic AI Service**: Intelligent supervisor that:
   - Determines optimal quiz difficulty
   - Orchestrates generation process
   - Evaluates student performance
   - Makes pedagogical decisions
   - Adapts to student progress

## 📊 Sample Data

The application initializes with 3 comprehensive courses:

1. **Introduction to Spring Boot**
   - Auto-configuration, starters, security, JPA
   - Best practices and architecture

2. **Artificial Intelligence Fundamentals**
   - Machine Learning, Deep Learning, NLP
   - LLMs, RAG, Computer Vision
   - AI Ethics and Responsible AI

3. **Advanced Java Programming**
   - Streams API, Lambda Expressions
   - Concurrency, Design Patterns
   - Performance Optimization

## ⚙️ Configuration

### Key Settings

Edit `src/main/resources/application.properties`:

```properties
# OpenAI Configuration
openai.api.key=${OPENAI_API_KEY:your-api-key-here}
openai.model=gpt-4o-mini
openai.temperature=0.7

# RAG Configuration
rag.chunk.size=500
rag.chunk.overlap=100
rag.max.results=5

# Agentic AI Configuration
agent.quiz.min.questions=5
agent.quiz.max.questions=15
agent.pass.threshold=70
```

## 🏗️ Architecture

### Layered Architecture

```
Controllers (Presentation)
    ↓
Services (Business Logic + AI)
    ↓
Repositories (Data Access)
    ↓
Domain Model (Entities)
    ↓
Database (H2)
```

### Security

- **Role-Based Access Control**: ADMINISTRATOR and STUDENT roles
- **Method-Level Security**: `@PreAuthorize` annotations
- **Spring Security**: Form-based authentication with BCrypt
- **CSRF Protection**: Enabled for all state-changing operations

## 📈 Future Enhancements

- Multi-format RAG (PDF, images, videos)
- Advanced dashboards with analytics
- Automatic course certification
- Multi-language support
- Enhanced agentic capabilities (learning path recommendations)
- Integration with external LMS platforms

## 📚 Documentation

For detailed technical documentation, see [TECHNICAL_DOCUMENTATION.md](TECHNICAL_DOCUMENTATION.md)

### Key Topics Covered
- Detailed architecture explanation
- Domain model and relationships
- Security implementation
- AI components deep dive
- Business logic rules
- API documentation
- Production deployment guide

## 🧪 Testing

Run tests:
```bash
mvnw test
```

Access H2 Console for database inspection:
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:pedagogical_platform
Username: sa
Password: (leave blank)
```

## 🔧 Troubleshooting

### Common Issues

**OpenAI API errors**: 
- Ensure API key is correctly set
- Check API quota and billing
- Application falls back to demo quiz if API unavailable

**Database errors**:
- H2 console at `/h2-console` for inspection
- Check `spring.jpa.show-sql=true` for SQL debugging

**Security issues**:
- Verify role assignments in database
- Check Spring Security logs: `logging.level.org.springframework.security=DEBUG`

## 🤝 Contributing

This is an educational demonstration project. To extend:

1. Follow the layered architecture pattern
2. Add tests for new features
3. Document AI decision logic
4. Update configuration as needed

## 📄 License

Educational and demonstration purposes.

## 🎓 Credits

Developed as a comprehensive demonstration of:
- Spring Boot expertise
- Spring Security implementation
- AI integration (LLM + RAG + Agentic AI)
- Clean architecture principles
- Production-ready development practices

---

**This is education, intelligently automated.** 🚀
