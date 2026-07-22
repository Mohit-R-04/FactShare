package com.factshare.controller;
import com.factshare.dto.SubmitArticleRequest;
import com.factshare.model.Article;
import com.factshare.service.ArticleService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController

public class ArticleController {
    private final ArticleService articleService;
    public ArticleController(ArticleService articleService) { this.articleService = articleService; }
    @PostMapping("/submit-article")
    public ResponseEntity<Article> submitArticle(Authentication auth, @RequestBody SubmitArticleRequest req) {
        return ResponseEntity.ok(articleService.submitArticle(auth.getName(), req));
    }
    @GetMapping("/article-history")
    public ResponseEntity<List<Article>> getHistory(Authentication auth) {
        return ResponseEntity.ok(articleService.getArticleHistory(auth.getName()));
    }
}
