package com.factshare.service;
import com.factshare.dto.SubmitArticleRequest;
import com.factshare.model.Article;
import com.factshare.repository.ArticleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final GeminiService geminiService;
    public ArticleService(ArticleRepository articleRepository, GeminiService geminiService) {
        this.articleRepository = articleRepository;
        this.geminiService = geminiService;
    }

    public Article submitArticle(String userId, SubmitArticleRequest req) {
        int score = 50;
        try {
            List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", "Score article credibility 0-100. Respond ONLY: {\"score\":number,\"reasoning\":\"brief\"}"),
                Map.of("role", "user", "content", "Article:\nTitle: " + (req.getTitle() != null ? req.getTitle() : "Untitled") + "\nContent: " + req.getContent())
            );
            String resp = geminiService.chat(messages, 256);
            int start = resp.indexOf('{');
            int end = resp.lastIndexOf('}');
            if (start >= 0 && end > start) {
                var node = new ObjectMapper().readTree(resp.substring(start, end + 1));
                score = Math.min(100, Math.max(0, node.path("score").asInt(50)));
            }
        } catch (Exception e) { e.printStackTrace(); }
        Article article = new Article();
        article.setUserId(userId);
        article.setType(req.getType());
        article.setTitle(req.getTitle() != null ? req.getTitle() : "Untitled");
        article.setContent(req.getContent());
        article.setCredibilityScore(score);
        article.setSubmissionDate(LocalDateTime.now());
        return articleRepository.save(article);
    }

    public List<Article> getArticleHistory(String userId) {
        return articleRepository.findByUserIdOrderBySubmissionDateDesc(userId);
    }

    /**
     * Persist a verification result (news claim / content check / image) into
     * the user's article history so the Dashboard reflects real activity.
     */
    public Article saveVerification(String userId, String type, String title, String content, int score) {
        Article article = new Article();
        article.setUserId(userId);
        article.setType(type);
        article.setTitle(title != null && !title.isBlank() ? title : "Untitled verification");
        article.setContent(content);
        article.setCredibilityScore(Math.min(100, Math.max(0, score)));
        article.setSubmissionDate(LocalDateTime.now());
        return articleRepository.save(article);
    }
}
