# CodeRefine ✦
> AI-powered code optimizer built with **Claude AI** by Anthropic.

Paste code in any language → get clean, production-grade code back in seconds.

## Features
- **Refine** — improves structure, naming, and adds documentation
- **Convert** — translates between Java, Python, JS, TS, C++, C#, Go, Rust
- **AI Tutor** — explains the refined code in plain English
- **Light / Dark mode** — toggle between themes

## Tech Stack
| Layer | Tech |
|---|---|
| Frontend | HTML, CSS, JavaScript |
| Backend | Java HttpServer |
| AI | LLaMA 3.3 70B via Groq |

## Setup
```bash
# 1. Add your Groq API key in AIServices.java
# 2. Compile
javac *.java
# 3. Run
java Server
# 4. Open in browser
http://localhost:3000
```
> Get a free API key at [console.groq.com](https://console.groq.com)

## Project Structure
```
CodeRefine/
├── Server.java       # HTTP server
├── AIServices.java   # Groq API + prompt engineering
├── Main.java         # CLI version
└── index.html        # Frontend UI
```

---
<p align="center">Made with ❤️ and <strong>Claude AI</strong></p>
