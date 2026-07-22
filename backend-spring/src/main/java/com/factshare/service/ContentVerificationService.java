package com.factshare.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.factshare.dto.VerifyContentRequest;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ContentVerificationService {
    private final MimoAiService mimoAiService;
    private ObjectMapper objectMapper = new ObjectMapper();
    public ContentVerificationService(MimoAiService mimoAiService) { this.mimoAiService = mimoAiService; }

    public Map<String, Object> verifyContent(VerifyContentRequest req) {
        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content",
                "Analyze content for authenticity. Respond with ONLY JSON: {\"extracted_text\":\"text\",\"verdict\":\"AUTHENTIC/MANIPULATED/MISLEADING/UNVERIFIABLE\",\"confidence\":0-100,\"explanation\":\"analysis\",\"flags\":[\"flag1\"]}"),
            Map.of("role", "user", "content", "Analyze for authenticity:\n\n" + req.getImageText())
        );
        try {
            String aiResponse = mimoAiService.chat(messages, 1024);
            String json = extractJson(aiResponse);
            if (json != null) {
                Map<String, Object> result = objectMapper.readValue(json, Map.class);
                result.putIfAbsent("extracted_text", req.getImageText());
                result.putIfAbsent("verdict", "UNVERIFIABLE");
                result.putIfAbsent("confidence", 50);
                result.putIfAbsent("explanation", "Analysis completed.");
                result.putIfAbsent("flags", List.of());
                return result;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Map.of("extracted_text", req.getImageText(), "verdict", "UNVERIFIABLE",
            "confidence", 50, "explanation", "Unable to analyze at this time.",
            "flags", List.of());
    }
    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return null;
    }
}
