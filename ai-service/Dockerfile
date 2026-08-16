FROM python:3.11-slim

WORKDIR /app

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY gemini_service.py .

EXPOSE 5002

ENV GEMINI_SERVICE_PORT=5002

CMD ["python3", "gemini_service.py"]
