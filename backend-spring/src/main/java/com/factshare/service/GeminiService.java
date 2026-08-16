package com.factshare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.*;

@Service
public class GeminiService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.service.url:http://localhost:5002}")
    private String geminiServiceUrl;

    public GeminiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String chat(List<Map<String, String>> messages, int maxTokens) {
        String systemInstruction = "";
        String userPrompt = "";

        for (Map<String, String> msg : messages) {
            String role = msg.get("role");
            String content = msg.get("content");
            if ("system".equalsIgnoreCase(role)) {
                systemInstruction = content;
            } else if ("user".equalsIgnoreCase(role)) {
                userPrompt = content;
            }
        }

        return generateContent(userPrompt, systemInstruction, null);
    }

    public String generateContent(String prompt, String systemInstruction, MultipartFile image) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        if (prompt != null) {
            body.add("prompt", prompt);
        }
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            body.add("system_instruction", systemInstruction);
        }

        if (image != null && !image.isEmpty()) {
            try {
                ByteArrayResource contentsAsResource = new ByteArrayResource(image.getBytes()) {
                    @Override
                    public String getFilename() {
                        return image.getOriginalFilename() != null ? image.getOriginalFilename() : "image.jpg";
                    }
                };
                body.add("image", contentsAsResource);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read image bytes", e);
            }
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                geminiServiceUrl + "/generate", requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("text").asText();
            } else {
                throw new RuntimeException("Gemini service returned status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Gemini service", e);
        }
    }
}
