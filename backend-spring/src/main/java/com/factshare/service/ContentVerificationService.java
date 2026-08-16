package com.factshare.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.factshare.dto.VerifyContentRequest;
import com.factshare.dto.VerifyNewsRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.*;

@Service
public class ContentVerificationService {
    private final GeminiService geminiService;
    private final NewsVerificationService newsService;
    private ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<String> VERDICTS = Set.of("AUTHENTIC", "MANIPULATED", "MISLEADING", "UNVERIFIABLE");
    public ContentVerificationService(GeminiService geminiService, NewsVerificationService newsService) {
        this.geminiService = geminiService;
        this.newsService = newsService;
    }

    public Map<String, Object> verifyContent(VerifyContentRequest req) {
        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", authenticitySystemPrompt()),
            Map.of("role", "user", "content", "Analyze for authenticity:\n\n" + req.getImageText())
        );
        try {
            String aiResponse = geminiService.chat(messages, 1024);
            String json = extractJson(aiResponse);
            if (json != null) {
                Map<String, Object> result = objectMapper.readValue(json, Map.class);
                result.putIfAbsent("extracted_text", req.getImageText());
                result.put("verdict", normalizeVerdict((String) result.get("verdict")));
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

    public Map<String, Object> verifyImage(MultipartFile image, String userId) {
        String systemInstruction = authenticitySystemPrompt();
        String userPrompt = "Analyze for authenticity.";
        try {
            String aiResponse = geminiService.generateContent(userPrompt, systemInstruction, image);
            String json = extractJson(aiResponse);
            if (json != null) {
                Map<String, Object> result = objectMapper.readValue(json, Map.class);
                result.putIfAbsent("extracted_text", "");
                result.put("verdict", normalizeVerdict((String) result.get("verdict")));
                result.putIfAbsent("confidence", 50);
                result.putIfAbsent("explanation", "Analysis completed.");
                result.putIfAbsent("flags", List.of());
                attachNewsAnalysis(result, userId);
                return result;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Map.of("extracted_text", "", "verdict", "UNVERIFIABLE",
            "confidence", 50, "explanation", "Unable to analyze at this time.",
            "flags", List.of());
    }

    /**
     * Fact-check the text extracted from the image with the full news
     * verification pipeline (Gemini + search evidence + credibility score).
     * Attached as "newsAnalysis"; failures degrade gracefully.
     */
    private void attachNewsAnalysis(Map<String, Object> result, String userId) {
        String extracted = result.get("extracted_text") instanceof String
            ? ((String) result.get("extracted_text")).trim() : "";
        if (extracted.isEmpty()) return;
        try {
            VerifyNewsRequest newsReq = new VerifyNewsRequest();
            newsReq.setClaim(extracted);
            result.put("newsAnalysis", newsService.verifyNews(newsReq, userId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The authoritative current date is injected at verification time so the
     * model never relies on its own (possibly stale) knowledge of today's date.
     */
    private String authenticitySystemPrompt() {
        return "CURRENT_DATE: " + LocalDate.now() + " (" + LocalDate.now().getDayOfWeek() + ")\n"
            + "You are an expert content authenticity analyst. CURRENT_DATE above is the authoritative reference date; "
            + "use it for every date check and never assume today's date from your own knowledge.\n"
            + "Rules:\n"
            + "- A date mismatch, an old event being reported again, or an unusual date is NOT evidence that the content "
            + "was altered. First verify the actual dates and compare them correctly against CURRENT_DATE.\n"
            + "- Only classify as MANIPULATED when there is concrete evidence the image or text itself was altered "
            + "(e.g. tampering artifacts or editing inconsistencies).\n"
            + "- Distinguish: AUTHENTIC (genuine and unmodified), MANIPULATED (the content itself was altered), "
            + "MISLEADING (genuine but presented deceptively or out of context), UNVERIFIABLE (cannot be determined).\n"
            + "- Never emit a 'future_date' flag unless a date in the content is strictly later than CURRENT_DATE.\n"
            + "Respond with ONLY JSON: {\"extracted_text\":\"text\",\"verdict\":\"AUTHENTIC/MANIPULATED/MISLEADING/UNVERIFIABLE\","
            + "\"confidence\":0-100,\"explanation\":\"analysis\",\"flags\":[\"flag1\"]}";
    }

    private String normalizeVerdict(String verdict) {
        if (verdict == null) return "UNVERIFIABLE";
        String v = verdict.trim().toUpperCase(Locale.ROOT);
        return VERDICTS.contains(v) ? v : "UNVERIFIABLE";
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return null;
    }
}
