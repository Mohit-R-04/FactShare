package com.factshare.controller;
import com.factshare.dto.*;
import com.factshare.service.NewsVerificationService;
import com.factshare.service.ContentVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<Map<String, Object>> verifyNews(@RequestBody VerifyNewsRequest req) {
        return ResponseEntity.ok(newsService.verifyNews(req));
    }
    @PostMapping("/verify-image")
    public ResponseEntity<Map<String, Object>> verifyContent(@RequestBody VerifyContentRequest req) {
        return ResponseEntity.ok(contentService.verifyContent(req));
    }
}
