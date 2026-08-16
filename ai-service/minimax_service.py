import os
import requests
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# NVIDIA NIM chat completions endpoint hosting minimaxai/minimax-m3
INVOKE_URL = os.getenv(
    "NVIDIA_INVOKE_URL", "https://integrate.api.nvidia.com/v1/chat/completions"
)
MODEL = os.getenv("MINIMAX_MODEL", "minimaxai/minimax-m3")

# Default generation params (from the NVIDIA sample snippet)
DEFAULT_TEMPERATURE = float(os.getenv("MINIMAX_TEMPERATURE", "1"))
DEFAULT_TOP_P = float(os.getenv("MINIMAX_TOP_P", "0.95"))
DEFAULT_MAX_TOKENS = int(os.getenv("MINIMAX_MAX_TOKENS", "8192"))


@app.route('/generate', methods=['POST'])
def generate():
    # 1. Check API Key
    api_key = os.getenv("NVIDIA_API_KEY")
    if not api_key:
        return jsonify({"error": "NVIDIA_API_KEY environment variable is missing. Please set it to proceed."}), 401

    prompt = None
    system_instruction = None
    max_tokens = None

    # 2. Parse request inputs (same contract as gemini_service.py)
    if request.content_type and 'multipart/form-data' in request.content_type:
        prompt = request.form.get('prompt')
        system_instruction = request.form.get('system_instruction')
        if request.form.get('max_tokens'):
            max_tokens = request.form.get('max_tokens')
        if 'image' in request.files and request.files['image'].filename != '':
            # minimax-m3 is a text model — image analysis stays on the Gemini service
            return jsonify({"error": "Minimax M3 is text-only; image input is not supported. Use the Gemini service for image analysis."}), 400
    else:
        data = request.get_json(silent=True) or {}
        prompt = data.get('prompt')
        system_instruction = data.get('system_instruction')
        max_tokens = data.get('max_tokens')

    # 3. Handle empty input
    if not prompt:
        return jsonify({"error": "Empty input: 'prompt' must be provided."}), 400

    try:
        max_tokens = int(max_tokens) if max_tokens else DEFAULT_MAX_TOKENS
    except (TypeError, ValueError):
        max_tokens = DEFAULT_MAX_TOKENS

    # 4. Build OpenAI-style chat messages
    messages = []
    if system_instruction:
        messages.append({"role": "system", "content": system_instruction})
    messages.append({"role": "user", "content": prompt})

    headers = {
        "Authorization": f"Bearer {api_key}",
        "Accept": "application/json",
    }

    payload = {
        "model": MODEL,
        "messages": messages,
        "temperature": DEFAULT_TEMPERATURE,
        "top_p": DEFAULT_TOP_P,
        "max_tokens": max_tokens,
        "stream": False,
    }

    # 5. Call NVIDIA NIM API
    try:
        response = requests.post(INVOKE_URL, headers=headers, json=payload, timeout=120)

        if response.status_code in (401, 403):
            return jsonify({"error": "Invalid NVIDIA API key."}), 401
        if response.status_code == 429:
            return jsonify({"error": "NVIDIA API rate limit exceeded. Please try again later."}), 429
        if response.status_code != 200:
            return jsonify({"error": f"NVIDIA API error ({response.status_code}): {response.text}"}), 502

        body = response.json()
        choices = body.get("choices") or []
        if not choices:
            return jsonify({"error": "Minimax returned an empty response."}), 502

        message = choices[0].get("message") or {}
        text = message.get("content")

        # Some NIM reasoning models put the final answer in `content` and the
        # chain-of-thought in `reasoning_content`; fall back just in case.
        if not text:
            text = message.get("reasoning_content")

        if not text:
            return jsonify({"error": "Minimax returned an empty response."}), 502

        # 6. Match the gemini_service response shape
        return jsonify({"text": text})

    except requests.exceptions.Timeout:
        return jsonify({"error": "NVIDIA API request timed out. Please try again later."}), 504
    except requests.exceptions.RequestException as e:
        return jsonify({"error": f"Failed to reach NVIDIA API: {str(e)}"}), 502
    except Exception as e:
        return jsonify({"error": f"Internal error during generation: {str(e)}"}), 500


if __name__ == '__main__':
    port = int(os.getenv("MINIMAX_SERVICE_PORT", 5003))
    # Bind to 0.0.0.0 so Flask is reachable inside Docker.
    # For local dev, access via http://localhost:5003
    host = os.getenv("MINIMAX_SERVICE_HOST", "0.0.0.0")
    app.run(host=host, port=port, debug=False)
