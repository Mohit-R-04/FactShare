package com.factshare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

/**
 * Proxies web search evidence to the Python ai-service, which is the only
 * component that holds the TAVILY_API_KEY. The backend never sees the key.
 */
@Service
public class TavilyService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.service.url:http://localhost:5002}")
    private String aiServiceUrl;

    public TavilyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Search the web via the ai-service /tavily/search endpoint.
     * Never throws: failures and empty results are returned as an empty
     * evidence payload so verification can degrade gracefully.
     */
    public Map<String, Object> search(String keyword) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("keyword", keyword), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                aiServiceUrl + "/tavily/search", entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
                if (body.containsKey("results")) {
                    return body;
                }
            }
        } catch (Exception e) {
            // Search evidence is optional; verification continues without it.
        }
        return Map.of("results", Map.of(), "total", 0, "error", "Search unavailable");
    }
}
