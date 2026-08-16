import os
from flask import Flask, request, jsonify
from flask_cors import CORS
from google import genai
from google.genai import types
from google.genai.errors import APIError
from PIL import Image
import requests

app = Flask(__name__)
CORS(app)

# Tavily Search API (official REST API). The key is read from the environment
# at request time only — never hardcoded.
TAVILY_API_URL = "https://api.tavily.com/search"
TAVILY_SEARCH_TIMEOUT = 12  # seconds; verification must not hang


@app.route('/tavily/search', methods=['POST'])
def tavily_search():
    # 1. API key lives only in the environment (never hardcoded)
    api_key = os.getenv("TAVILY_API_KEY")
    if not api_key:
        return jsonify({"error": "TAVILY_API_KEY environment variable is missing. Add it to .env to enable web search evidence."}), 401

    # 2. Parse the news claim as the search query
    data = request.get_json(silent=True) or {}
    query = (data.get("keyword") or "").strip()
    if not query:
        return jsonify({"error": "Empty input: 'keyword' is required."}), 400

    # 3. Call Tavily with the claim as the query. topic=news prefers recent
    #    sources (with published_date) for current events.
    try:
        resp = requests.post(
            TAVILY_API_URL,
            headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
            json={
                "query": query,
                "search_depth": "advanced",
                "topic": "news",
                "max_results": 8,
                "include_answer": False,
                "include_raw_content": False,
                "include_images": False,
            },
            timeout=TAVILY_SEARCH_TIMEOUT,
        )
    except requests.RequestException as e:
        return jsonify({"error": f"Tavily request failed: {str(e)}"}), 502

    # 4. Surface upstream errors with their message
    if resp.status_code != 200:
        msg = f"Tavily API error (HTTP {resp.status_code})"
        try:
            err = resp.json()
            detail = err.get("detail") if isinstance(err, dict) else None
            if isinstance(detail, dict) and detail.get("error"):
                msg = f"{msg}: {detail['error']}"
        except Exception:
            pass
        return jsonify({"error": msg}), 502

    # 5. Parse and return clean results (empty results -> 200 with empty buckets)
    try:
        body = resp.json()
    except Exception:
        return jsonify({"error": "Tavily returned an invalid response."}), 502

    items = body.get("results") if isinstance(body, dict) else None
    results = {"organic": []}
    for item in items or []:
        if not isinstance(item, dict):
            continue
        results["organic"].append({
            "title": item.get("title") or "",
            "url": item.get("url") or "",
            "description": item.get("content") or item.get("raw_content") or "",
            "domain": item.get("source") or "",
            "published_date": item.get("published_date") or "",
        })
    return jsonify({
        "results": results,
        "total": len(results["organic"]),
        "query": body.get("query") if isinstance(body, dict) else None,
    })

@app.route('/generate', methods=['POST'])
def generate():
    # 1. Check API Key
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        return jsonify({"error": "GEMINI_API_KEY environment variable is missing. Please set it to proceed."}), 401

    try:
        client = genai.Client(api_key=api_key)
    except Exception as e:
        return jsonify({"error": f"Failed to initialize Gemini Client: {str(e)}"}), 500

    prompt = None
    system_instruction = None
    image = None

    # 2. Parse request inputs
    if request.content_type and 'multipart/form-data' in request.content_type:
        prompt = request.form.get('prompt')
        system_instruction = request.form.get('system_instruction')
        if 'image' in request.files:
            image_file = request.files['image']
            if image_file.filename != '':
                try:
                    # Accept PIL.Image.Image directly by opening file stream
                    image = Image.open(image_file.stream)
                    # Force loading the image data to verify it is valid
                    image.verify()
                    # Re-open stream since verify() closes/invalidates the stream
                    image_file.stream.seek(0)
                    image = Image.open(image_file.stream)
                except Exception as e:
                    return jsonify({"error": f"Invalid image format: {str(e)}"}), 400
    else:
        data = request.get_json(silent=True) or {}
        prompt = data.get('prompt')
        system_instruction = data.get('system_instruction')

    # 3. Handle empty inputs (must have at least prompt or image)
    if not prompt and not image:
        return jsonify({"error": "Empty input: either 'prompt' or 'image' must be provided."}), 400

    # 4. Support text-only, image-only, text + image
    contents = []
    if image:
        contents.append(image)
    if prompt:
        contents.append(prompt)

    # 5. Call Gemini API (Free Tier: gemini-3.1-flash-lite)
    try:
        config = None
        if system_instruction:
            config = types.GenerateContentConfig(system_instruction=system_instruction)

        response = client.models.generate_content(
            model="gemini-3.1-flash-lite",
            contents=contents,
            config=config
        )

        if not response or not response.text:
            return jsonify({"error": "Gemini returned an empty response."}), 502

        # 6. Gemini returns text only
        return jsonify({"text": response.text})

    except APIError as e:
        # Handle invalid API key, rate limit, and general API errors
        err_msg = str(e)
        if "API_KEY_INVALID" in err_msg or "INVALID_ARGUMENT" in err_msg:
            return jsonify({"error": "Invalid Gemini API key."}), 401
        if "RESOURCE_EXHAUSTED" in err_msg or "429" in err_msg:
            return jsonify({"error": "Gemini API rate limit exceeded. Please try again later."}), 429
        return jsonify({"error": f"Gemini API error: {err_msg}"}), 502
    except Exception as e:
        return jsonify({"error": f"Internal error during generation: {str(e)}"}), 500

if __name__ == '__main__':
    port = int(os.getenv("GEMINI_SERVICE_PORT", 5002))
    # Bind to 0.0.0.0 so Flask is reachable inside Docker.
    # For local dev, access via http://localhost:5002
    host = os.getenv("GEMINI_SERVICE_HOST", "0.0.0.0")
    app.run(host=host, port=port, debug=False)
