package com.factshare.service;
import com.factshare.dto.ChatRequest;
import com.factshare.dto.ChatResponse;

import org.springframework.stereotype.Service;
import java.util.*;

@Service

public class ChatService {
    private final GeminiService geminiService;
    private final MinimaxService minimaxService;
    public ChatService(GeminiService geminiService, MinimaxService minimaxService) {
        this.geminiService = geminiService;
        this.minimaxService = minimaxService;
    }

    public ChatResponse chat(ChatRequest req) {
        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content",
                "You are FactBot, an AI news verification assistant. Help fact-check claims and explain media literacy. Format responses as HTML with <p>, <strong>, <ul><li> tags. Be concise and accurate."),
            Map.of("role", "user", "content", req.getQuestion())
        );
        try {
            String aiResponse;
            try {
                // Primary: use the Minimax (NVIDIA) service
                aiResponse = minimaxService.chat(messages, 1024);
            } catch (Exception minimaxError) {
                // Fallback: keep the existing Gemini path alive if Minimax is unavailable
                minimaxError.printStackTrace();
                aiResponse = geminiService.chat(messages, 1024);
            }
            String formatted = aiResponse;
            if (!aiResponse.contains("<p>") && !aiResponse.contains("<ul>")) {
                formatted = "<p>" + aiResponse.replace("\n\n", "</p><p>") + "</p>";
                formatted = formatted.replaceAll("[*][*](.*?)[*][*]", "<strong>$1</strong>");
            }
            return new ChatResponse(formatted);
        } catch (Exception e) {
            return new ChatResponse("<p>I'm having trouble connecting. Please try again later.</p>");
        }
    }
}
