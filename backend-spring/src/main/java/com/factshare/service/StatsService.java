package com.factshare.service;
import com.factshare.dto.DashboardStatsResponse;
import com.factshare.model.Article;
import com.factshare.repository.ArticleRepository;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service

public class StatsService {
    private final ArticleRepository articleRepository;
    public StatsService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public DashboardStatsResponse getUserStats(String userId) {
        List<Article> articles = articleRepository.findByUserIdOrderBySubmissionDateDesc(userId);
        int total = articles.size();
        int avg = total > 0 ? (int) articles.stream().mapToInt(Article::getCredibilityScore).average().orElse(0) : 0;
        Map<String, int[]> monthly = new LinkedHashMap<>();
        for (Article a : articles) {
            String month = a.getSubmissionDate().getMonth().toString().substring(0, 3);
            monthly.computeIfAbsent(month, k -> new int[]{0, 0});
            monthly.get(month)[0]++;
            monthly.get(month)[1] += a.getCredibilityScore();
        }
        List<Map<String, Object>> trend = monthly.entrySet().stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", e.getKey());
            m.put("credibility", e.getValue()[1] / Math.max(1, e.getValue()[0]));
            return m;
        }).collect(Collectors.toList());
        long high = articles.stream().filter(a -> a.getCredibilityScore() >= 80).count();
        long medium = articles.stream().filter(a -> a.getCredibilityScore() >= 50 && a.getCredibilityScore() < 80).count();
        long low = articles.stream().filter(a -> a.getCredibilityScore() < 50).count();
        List<Map<String, Object>> dist = List.of(
            Map.of("name", "High (80-100)", "score", (int) high),
            Map.of("name", "Medium (50-79)", "score", (int) medium),
            Map.of("name", "Low (0-49)", "score", (int) low));
        List<Map<String, Object>> recent = new ArrayList<>();
        for (Article a : articles.stream().limit(5).collect(Collectors.toList())) {
            Map<String, Object> m = new HashMap<>();
            m.put("_id", a.getId());
            m.put("title", a.getTitle());
            m.put("credibilityScore", a.getCredibilityScore());
            m.put("submissionDate", a.getSubmissionDate().toString());
            recent.add(m);
        }
        DashboardStatsResponse resp = new DashboardStatsResponse();
        resp.setTotalArticles(total);
        resp.setAvgCredibility(avg);
        resp.setCredibilityTrend(trend);
        resp.setScoreDistribution(dist);
        resp.setRecentArticles(recent);
        return resp;
    }
}
