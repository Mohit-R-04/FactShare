package com.factshare.controller;

import com.factshare.dto.*;
import com.factshare.service.ContentVerificationService;
import com.factshare.service.NewsVerificationService;
import com.factshare.service.OcrService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class VerifyController {
    private final NewsVerificationService newsService;
    private final ContentVerificationService contentService;
    private final OcrService ocrService;

    public VerifyController(NewsVerificationService newsService, ContentVerificationService contentService, OcrService ocrService) {
        this.newsService = newsService;
        this.contentService = contentService;
        this.ocrService = ocrService;
    }

    @PostMapping("/verify-news")
    public ResponseEntity<Map<String, Object>> verifyNews(@RequestBody VerifyNewsRequest req) {
        return ResponseEntity.ok(newsService.verifyNews(req));
    }

    @PostMapping("/verify-image")
    public ResponseEntity<Map<String, Object>> verifyContent(@RequestBody VerifyContentRequest req) {
        return ResponseEntity.ok(contentService.verifyContent(req));
    }

    /**
     * Accepts an image upload, extracts text via OCR, then analyzes it with Mimo AI.
     */
    @PostMapping("/verify-image-upload")
    public ResponseEntity<Map<String, Object>> verifyImageUpload(@RequestParam("image") MultipartFile image) {
        try {
            // Step 1: Extract text from image using Tesseract OCR
            String extractedText = ocrService.extractText(image);

            if (extractedText == null || extractedText.isBlank()) {
                return ResponseEntity.ok(Map.of(
                    "extracted_text", "",
                    "verdict", "UNVERIFIABLE",
                    "confidence", 0,
                    "explanation", "No readable text could be extracted from the image. Please ensure the image contains clear text.",
                    "flags", java.util.List.of()
                ));
            }

            // Step 2: Analyze extracted text with Mimo AI
            VerifyContentRequest req = new VerifyContentRequest();
            req.setImageText(extractedText.trim());
            Map<String, Object> result = contentService.verifyContent(req);

            // Ensure the extracted text is included in the response
            result.put("extracted_text", extractedText.trim());
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