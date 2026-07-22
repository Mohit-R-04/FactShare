package com.factshare.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.factshare.dto.VerifyNewsRequest;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class NewsVerificationService {
    private final MimoAiService mimoAiService;
    private ObjectMapper objectMapper = new ObjectMapper();
    public NewsVerificationService(MimoAiService mimoAiService) { this.mimoAiService = mimoAiService; }

    public Map<String, Object> verifyNews(VerifyNewsRequest req) {
        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content",
                "You are an expert fact-checker AI. Respond with ONLY a JSON object: {\"claim\":\"text\",\"verdict\":\"TRUE/FALSE/MISLEADING/UNVERIFIABLE\",\"confidence\":0-100,\"explanation\":\"analysis\",\"sources\":[\"basis\"],\"category\":\"type\"}"),
            Map.of("role", "user", "content", "Fact-check this claim:\n\n\"" + req.getClaim() + "\"")
        );
        try {
            String aiResponse = mimoAiService.chat(messages, 1024);
            String json = extractJson(aiResponse);
            if (json != null) {
                Map<String, Object> result = objectMapper.readValue(json, Map.class);
                result.putIfAbsent("claim", req.getClaim());
                result.putIfAbsent("verdict", "UNVERIFIABLE");
                result.putIfAbsent("confidence", 50);
                result.putIfAbsent("explanation", "Analysis completed.");
                result.putIfAbsent("sources", List.of("AI Analysis"));
                result.putIfAbsent("category", "other");
                return result;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Map.of("claim", req.getClaim(), "verdict", "UNVERIFIABLE",
            "confidence", 50, "explanation", "Unable to analyze at this time.",
            "sources", List.of("AI Analysis"), "category", "other");
    }
    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return null;
    }
}
