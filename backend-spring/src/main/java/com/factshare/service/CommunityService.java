package com.factshare.service;
import com.factshare.dto.VoteRequest;
import com.factshare.model.CommunityArticle;
import com.factshare.model.CommunityArticle.Voter;
import com.factshare.repository.CommunityArticleRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CommunityService {
    private final CommunityArticleRepository repository;
    public CommunityService(CommunityArticleRepository repository) { this.repository = repository; }

    public List<CommunityArticle> getAllArticles() {
        return repository.findAllByOrderBySubmissionDateDesc();
    }

    public CommunityArticle publishArticle(String userId, Map<String, Object> body) {
        CommunityArticle article = new CommunityArticle();
        article.setUserId(userId);
        article.setType((String) body.get("type"));
        article.setTitle((String) body.get("title"));
        article.setContent((String) body.get("content"));
        article.setCredibilityScore(body.containsKey("credibilityScore") ? ((Number) body.get("credibilityScore")).intValue() : 50);
        article.setSubmissionDate(LocalDateTime.now());
        article.setVotes(new CommunityArticle.Vote(0, 0));
        article.setVoters(new ArrayList<>());
        return repository.save(article);
    }

    public CommunityArticle vote(String userId, String articleId, VoteRequest req) {
        CommunityArticle article = repository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("Article not found"));
        if (article.getVoters() == null) article.setVoters(new ArrayList<>());
        if (article.getVotes() == null) article.setVotes(new CommunityArticle.Vote(0, 0));
        String voteType = req.getVoteType();
        Optional<Voter> existing = article.getVoters().stream()
            .filter(v -> v.getUserId().equals(userId)).findFirst();
        if (existing.isPresent()) {
            Voter v = existing.get();
            if (v.getVoteType().equals(voteType)) {
                if ("upvote".equals(voteType)) article.getVotes().setUpvotes(article.getVotes().getUpvotes() - 1);
                else article.getVotes().setDownvotes(article.getVotes().getDownvotes() - 1);
                article.getVoters().remove(v);
            } else {
                if ("upvote".equals(voteType)) {
                    article.getVotes().setUpvotes(article.getVotes().getUpvotes() + 1);
                    article.getVotes().setDownvotes(article.getVotes().getDownvotes() - 1);
                } else {
                    article.getVotes().setDownvotes(article.getVotes().getDownvotes() + 1);
                    article.getVotes().setUpvotes(article.getVotes().getUpvotes() - 1);
                }
                v.setVoteType(voteType);
            }
        } else {
            article.getVoters().add(new Voter(userId, voteType));
            if ("upvote".equals(voteType)) article.getVotes().setUpvotes(article.getVotes().getUpvotes() + 1);
            else article.getVotes().setDownvotes(article.getVotes().getDownvotes() + 1);
        }
        return repository.save(article);
    }
}
