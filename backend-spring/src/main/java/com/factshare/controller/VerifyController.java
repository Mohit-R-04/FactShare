package com.factshare.controller;

import com.factshare.dto.*;
import com.factshare.service.ContentVerificationService;
import com.factshare.service.NewsVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class VerifyController {
    private final NewsVerificationService newsService;
    private final ContentVerificationService contentService;
    public VerifyController(NewsVerificationService newsService, ContentVerificationService contentService) {
        this.newsService = newsService;
        this.contentService = contentService;
    }

    @PostMapping("/verify-news")
    public ResponseEntity<Map<String, Object>> verifyNews(Authentication auth, @RequestBody VerifyNewsRequest req) {
        String userId = (auth != null && !"anonymousUser".equals(auth.getName())) ? auth.getName() : "system";
        return ResponseEntity.ok(newsService.verifyNews(req, userId));
    }

    @PostMapping("/verify-image")
    public ResponseEntity<Map<String, Object>> verifyContent(Authentication auth, @RequestBody VerifyContentRequest req) {
        String userId = (auth != null && !"anonymousUser".equals(auth.getName())) ? auth.getName() : "system";
        return ResponseEntity.ok(contentService.verifyContent(req, userId));
    }

    /**
     * Accepts an image upload and analyzes it with Gemini.
     */
    @PostMapping("/verify-image-upload")
    public ResponseEntity<Map<String, Object>> verifyImageUpload(Authentication auth, @RequestParam("image") MultipartFile image) {
        try {
            String userId = (auth != null && !"anonymousUser".equals(auth.getName())) ? auth.getName() : "system";
            Map<String, Object> result = contentService.verifyImage(image, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "extracted_text", "",
                "verdict", "UNVERIFIABLE",
                "confidence", 0,
                "explanation", "Failed to process image: " + e.getMessage(),
                "flags", java.util.List.of()
            ));
        }
    }
}