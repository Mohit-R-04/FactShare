# FactShare AI Service

A lightweight standalone Python Flask microservice that bridges the Spring Boot backend to the Google Gemini API (`gemini-2.5-flash-lite`).

## Requirements

- Python 3.10+
- A valid `GEMINI_API_KEY` from [Google AI Studio](https://aistudio.google.com/)

## Setup

```bash
cd ai-service
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
```

## Run

```bash
export GEMINI_API_KEY="your-gemini-api-key"
.venv/bin/python gemini_service.py
```

Runs on `http://127.0.0.1:5002` by default. You can override via `GEMINI_SERVICE_PORT`.

## API

### `POST /generate`

Accepts `multipart/form-data` or `application/json`.

| Field               | Type             | Required            | Description                        |
|---------------------|------------------|---------------------|------------------------------------|
| `prompt`            | string           | Yes (or `image`)    | Text prompt to send to Gemini      |
| `system_instruction`| string           | No                  | System instruction for the model   |
| `image`             | file upload      | No                  | Image to analyze (multimodal)      |

Supports:
- Text only
- Image only
- Text + image

Returns:
```json
{"text": "Gemini response..."}
```

## Docker

```bash
docker build -t factshare-ai-service .
docker run -p 5002:5002 -e GEMINI_API_KEY="your-key" factshare-ai-service
```

## Minimax (NVIDIA NIM) microservice

`minimax_service.py` is a lightweight Flask microservice that forwards chat-completion
requests to the NVIDIA NIM endpoint hosting `minimaxai/minimax-m3`
(`https://integrate.api.nvidia.com/v1/chat/completions`). It exposes the **same
`POST /generate` contract** as `gemini_service.py` so it can be a drop-in text-only
backend for the Spring Boot chatbot.

It is **text-only** — image/multimodal verification still goes through the Gemini service.

### Requirements

- Python 3.10+
- A valid `NVIDIA_API_KEY` from [NVIDIA NIM](https://catalog.ngc.nvidia.com/api-keys)

### API

`POST /generate` — accepts `application/json` or `multipart/form-data`.

| Field              | Type   | Required | Description                       |
|--------------------|--------|----------|-----------------------------------|
| `prompt`           | string | Yes      | Text prompt                       |
| `system_instruction` | string | No       | System message                    |
| `max_tokens`       | int   | No       | Max output tokens (default 8192) |

Returns: `{"text": "Minimax response..."}`

### Run

```bash
export NVIDIA_API_KEY="your-nvidia-api-key"
.venv/bin/python minimax_service.py
```

Runs on `http://127.0.0.1:5003` by default (override via `MINIMAX_SERVICE_PORT`).

### Docker

```bash
docker build -f Dockerfile.minimax -t factshare-minimax-service .
docker run -p 5003:5003 -e NVIDIA_API_KEY="your-key" factshare-minimax-service
```
