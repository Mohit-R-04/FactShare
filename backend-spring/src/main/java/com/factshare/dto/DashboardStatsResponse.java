package com.factshare.dto;
import java.util.*;
public class DashboardStatsResponse {
    private int totalArticles; private int avgCredibility;
    private List<Map<String, Object>> credibilityTrend;
    private List<Map<String, Object>> scoreDistribution;
    private List<Map<String, Object>> recentArticles;
    public int getTotalArticles() { return totalArticles; }
    public void setTotalArticles(int v) { this.totalArticles = v; }
    public int getAvgCredibility() { return avgCredibility; }
    public void setAvgCredibility(int v) { this.avgCredibility = v; }
    public List<Map<String, Object>> getCredibilityTrend() { return credibilityTrend; }
    public void setCredibilityTrend(List<Map<String, Object>> v) { this.credibilityTrend = v; }
    public List<Map<String, Object>> getScoreDistribution() { return scoreDistribution; }
    public void setScoreDistribution(List<Map<String, Object>> v) { this.scoreDistribution = v; }
    public List<Map<String, Object>> getRecentArticles() { return recentArticles; }
    public void setRecentArticles(List<Map<String, Object>> v) { this.recentArticles = v; }
}
