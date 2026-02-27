1. Introduction

CodeRefine is a full-stack AI application that refactors raw source code into optimized, production-grade Java.

The system integrates a Java backend with the LLaMA 3.3 70B model via Groq to perform intelligent code transformation and optimization.

It converts any input language (Python, C++, JavaScript, etc.) into structured, maintainable Java following clean architecture principles.

2. Objective

The goal of this project is to:

Automate professional-level code refactoring

Improve time and space complexity

Apply clean code standards

Enforce object-oriented design

Generate production-ready Java

Provide structured explanation of improvements

3. System Architecture
High-Level Flow
Frontend (HTML/CSS/JS)
        ↓
Java HTTP Server (Port 8080)
        ↓
AIServices (AI Integration Layer)
        ↓
Groq API
        ↓
LLaMA 3.3 70B Model
4. Core Components
4.1 index-1.html (Frontend Layer)

Responsibilities:

Accept user code input

Send POST request to backend

Display AI-refined Java code

Separate improvements section

Provide syntax highlighting

Display line numbers

Provide copy functionality

API Call:

POST http://localhost:8080/refine
Content-Type: application/json

{
  "code": "user input code"
}
4.2 Server.java (HTTP Server Layer)

Responsibilities:

Create HTTP server on port 8080

Handle CORS for frontend access

Accept POST requests

Extract code from JSON body

Invoke AIServices.refineCode()

Return AI response in JSON format

This acts as the API gateway of the system.

4.3 AIServices.java (AI Service Layer)

Responsibilities:

Construct structured AI prompt

Enforce strict output formatting

Call Groq Chat Completions API

Use model: LLaMA 3.3 70B

Extract response content

Return clean Java output

AI Model Provider:

Groq

5. AI Prompt Engineering Strategy

The system prompt enforces:

Clean code principles

Meaningful naming conventions

Improved time complexity

Improved space complexity

OOP design patterns

Proper method separation

Industry-level comments

Edge case handling

Removal of redundant logic

Production-ready structure

Strict output rules:

Only valid Java code

Must compile

No markdown formatting

Append:

-----IMPROVEMENTS MADE-----

Followed by structured bullet points.

6. Request Lifecycle

Step-by-step execution:

User enters raw code

Frontend sends POST request

Server receives request

Code extracted from JSON

AIServices builds AI prompt

Request sent to Groq

LLaMA generates optimized Java

Response returned to backend

JSON content extracted

Frontend displays:

Refactored Java

Improvements summary

7. Technology Stack

Backend:

Java 17+

HttpServer

HttpClient

REST Architecture

Frontend:

HTML5

Modern CSS

Vanilla JavaScript

AI Layer:

LLaMA 3.3 70B

Groq API

8. Security Considerations

Current Implementation:

API key stored in source code (development mode only)

Recommended Improvements:

Use environment variables

Add .env configuration

Implement request validation

Add rate limiting

Use proper JSON parser (Gson/Jackson)

9. Limitations

No authentication

No request throttling

Basic JSON parsing (manual extraction)

Localhost deployment only

10. Future Enhancements

Cloud deployment

User authentication

Multi-model support

File upload support

Automated test generation

Code complexity analyzer

GitHub integration

CI/CD automation

11. Use Cases

Learning clean Java practices

Code migration between languages

Automated code review

Hackathon AI projects

Developer productivity tool

12. Conclusion

CodeRefine demonstrates:

Full-stack integration

AI prompt engineering

Backend API design

Real-world LLM integration

Structured output parsing

Production-style system thinking

It is a practical implementation of AI-assisted software engineering.
