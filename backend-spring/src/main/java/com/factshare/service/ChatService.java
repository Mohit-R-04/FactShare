package com.factshare.service;
import com.factshare.dto.ChatRequest;
import com.factshare.dto.ChatResponse;

import org.springframework.stereotype.Service;
import java.util.*;

@Service

public class ChatService {
    private final MimoAiService mimoAiService;
    public ChatService(MimoAiService mimoAiService) {
        this.mimoAiService = mimoAiService;
    }

    public ChatResponse chat(ChatRequest req) {
        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content",
                "You are FactBot, an AI news verification assistant. Help fact-check claims and explain media literacy. Format responses as HTML with <p>, <strong>, <ul><li> tags. Be concise and accurate."),
            Map.of("role", "user", "content", req.getQuestion())
        );
        try {
            String aiResponse = mimoAiService.chat(messages, 1024);
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
