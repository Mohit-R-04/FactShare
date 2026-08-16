package com.factshare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class MinimaxService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${minimax.service.url:http://localhost:5003}")
    private String minimaxServiceUrl;

    public MinimaxService(RestTemplate restTemplate) {
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

        return generateContent(userPrompt, systemInstruction, maxTokens);
    }

    public String generateContent(String prompt, String systemInstruction, int maxTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        if (prompt != null) {
            body.put("prompt", prompt);
        }
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            body.put("system_instruction", systemInstruction);
        }
        if (maxTokens > 0) {
            body.put("max_tokens", maxTokens);
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                minimaxServiceUrl + "/generate", requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("text").asText();
            } else {
                throw new RuntimeException("Minimax service returned status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Minimax service", e);
        }
    }
}
