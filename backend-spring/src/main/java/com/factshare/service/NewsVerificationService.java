package com.factshare.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.factshare.dto.VerifyNewsRequest;
import com.factshare.model.CommunityArticle;
import com.factshare.model.NewsCategory;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service
public class NewsVerificationService {
    private final GeminiService geminiService;
    private final TavilyService tavilyService;
    private final CommunityService communityService;
    private ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<String> VERDICTS = Set.of(
        "TRUE", "FALSE", "MISLEADING", "UNVERIFIABLE", "UNVERIFIED", "SEARCH_UNAVAILABLE");

    public NewsVerificationService(GeminiService geminiService, TavilyService tavilyService,
                                   CommunityService communityService) {
        this.geminiService = geminiService;
        this.tavilyService = tavilyService;
        this.communityService = communityService;
    }

    public Map<String, Object> verifyNews(VerifyNewsRequest req, String userId) {
        // 1. Tavily web search is MANDATORY and always runs before any verdict —
        //    never skipped, even when Gemini could answer from memory alone.
        Map<String, Object> evidence = tavilyService.search(req.getClaim());
        boolean searchFailed = evidence.containsKey("error") || !evidence.containsKey("results");
        Map<String, Object> results = evidence.get("results") instanceof Map
            ? (Map<String, Object>) evidence.get("results") : Map.of();
        int total = evidence.get("total") instanceof Number
            ? ((Number) evidence.get("total")).intValue() : 0;

        // 2. No usable search evidence -> never pretend the claim was verified.
        Map<String, Object> result;
        if (searchFailed) {
            result = unverifiedResult(req.getClaim(), "SEARCH_UNAVAILABLE",
                "Web search evidence could not be retrieved, so this claim was NOT verified from AI knowledge alone. "
                + "It has been flagged for community review.");
        } else if (total == 0) {
            result = unverifiedResult(req.getClaim(), "UNVERIFIED",
                "Web search returned no relevant results for this claim. Without search evidence it cannot be "
                + "verified; it has been flagged for community review.");
        } else {
            // 3. Search evidence exists: Gemini MUST analyze it before deciding the verdict.
            Map<String, Object> initial = geminiAnalyze(req.getClaim());
            Map<String, Object> finalResult = geminiWithEvidence(req.getClaim(), initial, results);
            if (finalResult == null) {
                result = unverifiedResult(req.getClaim(), "UNVERIFIED",
                    "Search results were retrieved but the evidence analysis failed, so the claim could not be "
                    + "verified. It has been flagged for community review.");
            } else {
                result = finalResult;
                // 4. Always surface the relevant search source URLs.
                if (sourcesMissing(result)) {
                    List<String> urls = collectSourceUrls(results);
                    result.put("sources", urls.isEmpty() ? List.of("AI Analysis") : urls);
                }
            }
        }

        // 5. Normalize category and clamp credibility score to 0-100
        String category = NewsCategory.normalize((String) result.get("category"));
        result.put("category", category);
        int score = extractScore(result);
        result.put("credibilityScore", score);
        result.put("credibilityScoreSource", total > 0 ? "evidence" : "none");
        result.put("searchEvidence", evidence);

        // 6. Low credibility -> Needs Community Review + auto-publish to Community Feed
        if (score <= 60) {
            try {
                CommunityArticle feedArticle = communityService.publishVerification(result, userId);
                result.put("communityFeed", true);
                result.put("communityArticleId", feedArticle.getId());
                result.put("reviewStatus", "NEEDS_REVIEW");
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        result.put("communityFeed", false);
        return result;
    }

    private Map<String, Object> unverifiedResult(String claim, String verdict, String explanation) {
        Map<String, Object> result = new HashMap<>();
        result.put("claim", claim);
        result.put("verdict", verdict);
        result.put("confidence", 0);
        result.put("explanation", explanation);
        result.put("sources", List.of());
        result.put("category", "Other");
        result.put("credibilityScore", 40); // unverified -> low -> community review
        return result;
    }

    private boolean sourcesMissing(Map<String, Object> result) {
        Object sources = result.get("sources");
        return sources == null || (sources instanceof List && ((List<?>) sources).isEmpty());
    }

    private List<String> collectSourceUrls(Map<String, Object> results) {
        List<String> urls = new ArrayList<>();
        for (Map.Entry<String, Object> entry : results.entrySet()) {
            if (!(entry.getValue() instanceof List)) continue;
            for (Object item : (List<?>) entry.getValue()) {
                if (!(item instanceof Map)) continue;
                Object url = ((Map<?, ?>) item).get("url");
                if (url instanceof String && !((String) url).isBlank() && urls.size() < 6) {
                    urls.add((String) url);
                }
            }
        }
        return urls;
    }

    private Map<String, Object> geminiAnalyze(String claim) {
        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", newsSystemPrompt()),
            Map.of("role", "user", "content", "Fact-check this claim:\n\n\"" + claim + "\"")
        );
        try {
            String aiResponse = geminiService.chat(messages, 1024);
            String json = extractJson(aiResponse);
            if (json != null) {
                Map<String, Object> result = objectMapper.readValue(json, Map.class);
                result.putIfAbsent("claim", claim);
                result.put("verdict", normalizeVerdict((String) result.get("verdict")));
                result.putIfAbsent("confidence", 50);
                result.putIfAbsent("explanation", "Analysis completed.");
                result.putIfAbsent("sources", List.of("AI Analysis"));
                result.putIfAbsent("category", "other");
                return result;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Map.of("claim", claim, "verdict", "UNVERIFIABLE",
            "confidence", 50, "explanation", "Unable to analyze at this time.",
            "sources", List.of("AI Analysis"), "category", "other");
    }

    /**
     * Second Gemini pass: the initial analysis plus live search results are
     * used as evidence for the final verdict and a 0-100 credibility score.
     */
    private Map<String, Object> geminiWithEvidence(String claim, Map<String, Object> initial, Map<String, Object> results) {
        String systemInstruction =
            newsSystemPrompt() + "\n"
            + "Evidence rules — the Google search results above are your PRIMARY evidence:\n"
            + "- Analyze the search results BEFORE deciding the verdict and credibility score; never decide from "
            + "pretrained knowledge alone, especially for current or recent events.\n"
            + "- If multiple recent credible sources support the claim, treat that as strong supporting evidence — do NOT "
            + "mark it FALSE; only mark FALSE when credible sources contradict it.\n"
            + "- Only state that 'there are no credible reports' if the provided search results actually contain no "
            + "relevant credible sources.\n"
            + "- Include the relevant source URLs from the search results in 'sources'.\n"
            + "You are given a claim, an initial AI analysis, and live Google search results. "
            + "Use the search results as evidence to reach a final verdict. "
            + "Respond with ONLY a JSON object: "
            + "{\"verdict\":\"TRUE/FALSE/MISLEADING/UNVERIFIABLE\",\"confidence\":0-100,\"credibilityScore\":0-100,\"explanation\":\"analysis citing evidence\",\"sources\":[\"https://real-source-url\"],\"category\":\"type\"}";

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Claim:\n\"").append(claim).append("\"\n\n");
        userPrompt.append("Initial analysis:\n").append(initial).append("\n\n");
        userPrompt.append("Google search results (type: title | url | snippet):\n");
        for (Map.Entry<String, Object> entry : results.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof List)) continue;
            List<?> list = (List<?>) value;
            if (list.isEmpty()) continue;
            userPrompt.append("[").append(entry.getKey()).append("]\n");
            for (Object item : list) {
                if (item instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) item;
                    Object title = m.get("title");
                    Object url = m.get("url");
                    Object published = m.get("published_date");
                    Object description = m.get("description");
                    String date = published == null || String.valueOf(published).isBlank() ? "n/a" : String.valueOf(published);
                    userPrompt.append("- ").append(title == null ? "" : title)
                        .append(" | ").append(url == null ? "" : url)
                        .append(" | published: ").append(date)
                        .append(" | ").append(description == null ? "" : description).append("\n");
                }
            }
        }

        try {
            List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemInstruction),
                Map.of("role", "user", "content", userPrompt.toString())
            );
            String aiResponse = geminiService.chat(messages, 1024);
            String json = extractJson(aiResponse);
            if (json != null) {
                Map<String, Object> result = objectMapper.readValue(json, Map.class);
                result.putIfAbsent("claim", claim);
                result.putIfAbsent("verdict", initial.get("verdict"));
                result.put("verdict", normalizeVerdict((String) result.get("verdict")));
                result.putIfAbsent("confidence", initial.get("confidence"));
                result.putIfAbsent("explanation", initial.get("explanation"));
                result.putIfAbsent("sources", initial.get("sources"));
                result.putIfAbsent("category", initial.get("category"));
                return result;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    /**
     * The authoritative current date is injected at verification time so the
     * model never relies on its own (possibly stale) knowledge of today's date.
     */
    private String newsSystemPrompt() {
        return "CURRENT_DATE: " + LocalDate.now() + " (" + LocalDate.now().getDayOfWeek() + ")\n"
            + "You are an expert fact-checker AI. CURRENT_DATE above is the authoritative reference date; "
            + "use it for every date check and never assume today's date from your own knowledge.\n"
            + "Rules:\n"
            + "- A date mismatch, an old event being reported again, or an unusual date is NOT by itself proof the claim is "
            + "false or misleading. First verify the actual dates and compare them correctly against CURRENT_DATE.\n"
            + "- Distinguish: TRUE (verified), FALSE (contradicted), MISLEADING (true but presented to deceive or out of "
            + "context), UNVERIFIABLE (cannot be determined).\n"
            + "- Never flag a date as 'future_date' unless it is strictly later than CURRENT_DATE.\n"
            + "Respond with ONLY a JSON object: {\"claim\":\"text\",\"verdict\":\"TRUE/FALSE/MISLEADING/UNVERIFIABLE\","
            + "\"confidence\":0-100,\"explanation\":\"analysis\",\"sources\":[\"basis\"],\"category\":\"type\"}";
    }

    private String normalizeVerdict(String verdict) {
        if (verdict == null) return "UNVERIFIABLE";
        String v = verdict.trim().toUpperCase(Locale.ROOT);
        return VERDICTS.contains(v) ? v : "UNVERIFIABLE";
    }

    private int extractScore(Map<String, Object> result) {
        Object scoreObj = result.get("credibilityScore");
        if (scoreObj instanceof Number) {
            return Math.min(100, Math.max(0, ((Number) scoreObj).intValue()));
        }
        // No evidence-backed score: derive credibility from the verdict.
        // Gemini's "confidence" is certainty in the verdict, not claim
        // credibility — a confidently debunked claim must score low.
        int confidence = result.get("confidence") instanceof Number
            ? ((Number) result.get("confidence")).intValue() : 50;
        String verdict = String.valueOf(result.getOrDefault("verdict", "")).toUpperCase();
        int score = switch (verdict) {
            case "TRUE" -> confidence;
            case "FALSE" -> 100 - confidence;
            case "MISLEADING" -> Math.max(0, 50 - confidence / 2);
            default -> Math.min(confidence, 50); // UNVERIFIABLE and anything else
        };
        return Math.min(100, Math.max(0, score));
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return null;
    }
}
