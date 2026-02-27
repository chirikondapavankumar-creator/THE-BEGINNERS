##🚀 CodeRefine

AI-Powered Java Code Refactoring Engine
Convert Any Language → Production-Grade Java

📌 Overview
CodeRefine is a full-stack AI application that refactors raw code into optimized, clean, production-ready Java using:

:LLaMA 3.3 70B

:Powered by Groq

:Java backend (HttpServer)

:Custom dual-pane frontend editor

index-1.html (Frontend)
        │
        ▼
Server.java (HTTP Server :8080)
        │
        ▼
AIServices.java (AI Layer)
        │
        ▼
Groq API → LLaMA 3.3 70B

CodeRefine/
│
├── AIServices.java      # AI integration layer
├── Server.java          # REST server
├── Main.java            # CLI version
├── index-1.html         # Frontend UI
└── README.md

POST http://localhost:8080/refine
Content-Type: application/json

{
  "code": "your raw code here"
}
HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
server.createContext("/refine", new CodeHandler());
server.start();

Extracts user code

Calls AIServices.refineCode()

Returns JSON response

3️⃣ AIServices.java calls Groq
private static final String API_URL =
    "https://api.groq.com/openai/v1/chat/completions";
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(API_URL))
    .header("Content-Type", "application/json")
    .header("Authorization", "Bearer " + API_KEY)
    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
    .build();

Model Used:

llama-3.3-70b-versatile

Provider:

Groq API
🧠 AI Prompt Rules

The backend enforces:

1. Clean Code Principles
2. Meaningful Naming
3. OOP Structure
4. Edge Case Handling
5. Time & Space Optimization
6. Professional Comments
7. No Markdown Output
8. Must Compile
9. Add Improvements Section

Required Output Format:

<Valid Java Code>

-----IMPROVEMENTS MADE-----
- Optimization details
- Structural improvements
- Complexity upgrades
🚀 Run Locally
🔹 Compile
javac *.java
🔹 Start Server
java Server

Output:

Server started at http://localhost:8080
🔹 Open Frontend

Open:

index-1.html
💻 Tech Stack
Backend  : Java 17+, HttpServer, HttpClient
Frontend : HTML5, CSS3, Vanilla JS
AI       : LLaMA 3.3 70B via Groq
🔐 Security Warning
⚠ Do NOT push your API key to GitHub.

Recommended:

export GROQ_API_KEY=your_key

Then read from:

System.getenv("GROQ_API_KEY");
📈 Future Improvements
- Cloud Deployment
- Auth System
- File Upload
- Test Case Generator
- Complexity Analyzer
- Multi-Model Support
