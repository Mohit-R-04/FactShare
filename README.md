# FactShare

FactShare is a news and image fact-checking web application. It consists of three separate services:

| Service | Stack | Port |
|---|---|---|
| **backend-spring** | Java 21, Spring Boot, MongoDB | `5001` |
| **ai-service** | Python 3, Flask, Google Gemini API | `5002` |
| **frontend** | React 19 | `3000` |

---

## Architecture

```
Browser → React Frontend (port 3000)
               ↓
Spring Boot Backend (port 5001)
               ↓
Python AI Microservice (port 5002)
               ↓
Google Gemini API (gemini-2.5-flash-lite)
```

- The **frontend** sends requests directly to the Spring Boot backend.
- The **Spring Boot backend** calls the **AI service** to get Gemini responses and Tavily search evidence.
- The **AI service** is the only component that holds the `GEMINI_API_KEY` and `TAVILY_API_KEY`.

Verification flow: Claim → Tavily web search (always) → search evidence →
Gemini evidence-backed verdict + credibility score (0–100) → score ≤ 60 is
marked **Untrusted / Needs Community Review** and auto-published to the
Community Feed, where journalists/community reviewers vote True / False /
Uncertain to update the final confidence and verdict.

---

## Prerequisites

| Tool | Minimum Version | Check |
|---|---|---|
| Java JDK | 21 | `java --version` |
| Maven | 3.9+ | `mvn -v` |
| Node.js + npm | 18+ | `node -v` |
| Python | 3.10+ | `python3 --version` |
| MongoDB | 4.4+ | `mongod --version` |

---

## Configuration

All environment-specific settings live in **one root `.env` file**:

```env
# MongoDB Connection
MONGO_URI=mongodb://localhost:27017/factshare

# JWT Secret (Spring Boot backend)
JWT_SECRET=your_jwt_secret_key_here

# Spring Boot Server Port
PORT=5001

# Python AI Microservice URL (Spring Boot reads this to call the AI service)
GEMINI_SERVICE_URL=http://localhost:5002

# Google Gemini API Key (Python AI service reads this to call Gemini)
# Get yours at: https://aistudio.google.com/
GEMINI_API_KEY=your_gemini_api_key_here

# Serper API Key (optional, for news search in chatbot)
SERPER_API_KEY=

# Tavily Search API Key (Python AI service reads this for
# news-verification search evidence). Get one at: https://tavily.com
TAVILY_API_KEY=your_tavily_key_here
```

> **Note**: The `GEMINI_API_KEY` is only ever read by the **Python `ai-service`**, not by the Java backend. The Java backend only needs `GEMINI_SERVICE_URL` to find and call the Python service.

---

## Setup & Running Locally

### 1. Database

Start MongoDB:
```bash
# macOS
brew services start mongodb-community

# Linux
sudo systemctl start mongod
```

### 2. AI Service (Python — Gemini bridge)

```bash
cd ai-service
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt

# Export your Gemini API key
export GEMINI_API_KEY="your_gemini_api_key_here"

.venv/bin/python gemini_service.py
```
→ Runs at `http://localhost:5002`

### 3. Backend (Java Spring Boot)

In a new terminal:
```bash
cd backend-spring

# Make sure GEMINI_SERVICE_URL and other vars are exported
export GEMINI_SERVICE_URL=http://localhost:5002
export MONGO_URI=mongodb://localhost:27017/factshare

mvn spring-boot:run
```
→ Runs at `http://localhost:5001`

### 4. Frontend (React)

In a new terminal:
```bash
cd frontend
npm install
npm start
```
→ Opens at `http://localhost:3000`

---

## Production Deployment

### Render (via `render.yaml`)

Three separate services are configured in `render.yaml`:

| Service Name | Type | Env Vars Required |
|---|---|---|
| `factshare-api` | Docker (Java) | `MONGO_URI`, `JWT_SECRET`, `CORS_ORIGINS`, `GEMINI_SERVICE_URL` |
| `factshare-ai-service` | Docker (Python) | `GEMINI_API_KEY` |
| `factshare-frontend` | Static (React) | — |

### Caddy Reverse Proxy

A `Caddyfile` is included for serving the app on `factshare.ssnce.dev`, proxying API routes to the Java backend on port `5001`.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `GEMINI_API_KEY` not set | Export it before running the ai-service: `export GEMINI_API_KEY="..."` |
| AI service returns 401 | Invalid API key — check the key at [aistudio.google.com](https://aistudio.google.com/) |
| AI service returns 429 | Gemini Free Tier rate limit hit — wait a minute and retry |
| Spring Boot can't reach ai-service | Check `GEMINI_SERVICE_URL` is correct (default: `http://localhost:5002`) |
| MongoDB connection refused | Start MongoDB: `brew services start mongodb-community` |
| Port already in use | `lsof -i :PORT` to identify and kill conflicting process |
