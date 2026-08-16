package com.factshare.service;
import com.factshare.dto.VoteRequest;
import com.factshare.model.CommunityArticle;
import com.factshare.model.CommunityArticle.ReviewVotes;
import com.factshare.model.CommunityArticle.Voter;
import com.factshare.repository.CommunityArticleRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommunityService {
    private final CommunityArticleRepository repository;
    public CommunityService(CommunityArticleRepository repository) { this.repository = repository; }

    /** Feed with filters: category, review status, credibility range, sort recent|disputed. */
    public List<CommunityArticle> getArticles(String category, String status, Integer minScore, Integer maxScore, String sort) {
        List<CommunityArticle> articles = repository.findAllByOrderBySubmissionDateDesc().stream()
            .filter(a -> category == null || category.isBlank() || category.equalsIgnoreCase(a.getCategory()))
            .filter(a -> status == null || status.isBlank() || status.equalsIgnoreCase(a.getReviewStatus()))
            .filter(a -> minScore == null || a.getCredibilityScore() >= minScore)
            .filter(a -> maxScore == null || a.getCredibilityScore() <= maxScore)
            .collect(Collectors.toList());
        if ("disputed".equalsIgnoreCase(sort)) {
            articles.sort(Comparator.comparingInt(CommunityArticle::getDisputeCount).reversed()
                .thenComparing(CommunityArticle::getSubmissionDate, Comparator.reverseOrder()));
        }
        return articles;
    }

    public List<CommunityArticle> getAllArticles() {
        return repository.findAllByOrderBySubmissionDateDesc();
    }

    public CommunityArticle publishArticle(String userId, Map<String, Object> body) {
        CommunityArticle article = new CommunityArticle();
        article.setUserId(userId);
        article.setType((String) body.get("type"));
        article.setTitle((String) body.get("title"));
        article.setContent((String) body.get("content"));
        article.setCategory(body.containsKey("category") ? (String) body.get("category") : "Other");
        article.setCredibilityScore(body.containsKey("credibilityScore") ? ((Number) body.get("credibilityScore")).intValue() : 50);
        article.setAiScore(article.getCredibilityScore());
        article.setSubmissionDate(LocalDateTime.now());
        article.setVotes(new CommunityArticle.Vote(0, 0));
        article.setVoters(new ArrayList<>());
        article.setCommunityVotes(new ReviewVotes(0, 0, 0));
        article.setReviewStatus("OPEN");
        return repository.save(article);
    }

    /**
     * Auto-publish a verification result with a low credibility score
     * (<= 60) to the Community Feed for review.
     */
    public CommunityArticle publishVerification(Map<String, Object> result, String userId) {
        String claim = String.valueOf(result.getOrDefault("claim", "Untitled claim"));
        CommunityArticle article = new CommunityArticle();
        article.setUserId(userId != null ? userId : "system");
        article.setType("verification");
        article.setTitle(claim.length() > 140 ? claim.substring(0, 140) : claim);
        article.setClaim(claim);
        article.setContent(String.valueOf(result.getOrDefault("explanation", "Awaiting community review.")));
        article.setCategory(String.valueOf(result.getOrDefault("category", "Other")));
        article.setVerdict(String.valueOf(result.getOrDefault("verdict", "UNVERIFIABLE")));
        int score = result.get("credibilityScore") instanceof Number
            ? ((Number) result.get("credibilityScore")).intValue() : 50;
        article.setCredibilityScore(Math.min(100, Math.max(0, score)));
        article.setAiScore(article.getCredibilityScore());
        article.setCommunityConfidence(article.getCredibilityScore());
        article.setSubmissionDate(LocalDateTime.now());
        article.setVotes(new CommunityArticle.Vote(0, 0));
        article.setVoters(new ArrayList<>());
        article.setCommunityVotes(new ReviewVotes(0, 0, 0));
        article.setReviewStatus("NEEDS_REVIEW");
        article.setDisputeCount(0);
        return repository.save(article);
    }

    public CommunityArticle vote(String userId, String articleId, VoteRequest req) {
        CommunityArticle article = repository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("Article not found"));
        if (article.getVoters() == null) article.setVoters(new ArrayList<>());
        if (article.getVotes() == null) article.setVotes(new CommunityArticle.Vote(0, 0));
        String voteType = req.getVoteType() == null ? "" : req.getVoteType().toLowerCase();
        switch (voteType) {
            case "true", "false", "uncertain" -> reviewVote(article, userId, voteType);
            case "upvote", "downvote" -> legacyVote(article, userId, voteType);
            default -> throw new RuntimeException("Invalid vote type: " + req.getVoteType());
        }
        return repository.save(article);
    }

    /** Community review vote: one vote per reviewer, changing a vote is allowed. */
    private void reviewVote(CommunityArticle article, String userId, String voteType) {
        if (article.getCommunityVotes() == null) article.setCommunityVotes(new ReviewVotes(0, 0, 0));
        ReviewVotes rv = article.getCommunityVotes();

        Optional<Voter> existing = article.getVoters().stream()
            .filter(v -> v.getUserId().equals(userId)).findFirst();
        if (existing.isPresent()) {
            Voter voter = existing.get();
            if (voter.getVoteType().equals(voteType)) {
                // Un-vote
                decrement(rv, voteType);
                article.getVoters().remove(voter);
            } else {
                decrement(rv, voter.getVoteType());
                increment(rv, voteType);
                voter.setVoteType(voteType);
            }
        } else {
            article.getVoters().add(new Voter(userId, voteType));
            increment(rv, voteType);
        }

        recomputeConfidence(article);
    }

    private void increment(ReviewVotes rv, String voteType) {
        switch (voteType) {
            case "true" -> rv.setTrueVotes(rv.getTrueVotes() + 1);
            case "false" -> rv.setFalseVotes(rv.getFalseVotes() + 1);
            case "uncertain" -> rv.setUncertainVotes(rv.getUncertainVotes() + 1);
        }
    }

    private void decrement(ReviewVotes rv, String voteType) {
        switch (voteType) {
            case "true" -> rv.setTrueVotes(Math.max(0, rv.getTrueVotes() - 1));
            case "false" -> rv.setFalseVotes(Math.max(0, rv.getFalseVotes() - 1));
            case "uncertain" -> rv.setUncertainVotes(Math.max(0, rv.getUncertainVotes() - 1));
        }
    }

    /**
     * Blend the AI score with community votes. Community weight grows with
     * participation but is capped at 35%, so a single vote can never decide
     * the outcome. The final verdict only flips to the community majority
     * once at least 3 reviewers have voted.
     */
    private void recomputeConfidence(CommunityArticle article) {
        ReviewVotes rv = article.getCommunityVotes();
        int total = rv.total();
        int aiScore = Math.max(0, article.getAiScore());
        double communityWeight = Math.min(0.35, total * 0.05);
        double truePct = total == 0 ? aiScore : (rv.getTrueVotes() / (double) total) * 100.0;
        double blended = aiScore * (1.0 - communityWeight) + truePct * communityWeight;
        int newScore = (int) Math.round(blended);
        article.setCommunityConfidence(blended);
        article.setCredibilityScore(Math.min(100, Math.max(0, newScore)));
        article.setDisputeCount(rv.getFalseVotes() + rv.getUncertainVotes());

        if (total >= 3) {
            String majority = rv.getTrueVotes() >= rv.getFalseVotes() && rv.getTrueVotes() >= rv.getUncertainVotes()
                ? "TRUE"
                : rv.getFalseVotes() >= rv.getUncertainVotes() ? "FALSE" : "UNVERIFIABLE";
            article.setVerdict(majority);
            article.setReviewStatus("REVIEWED");
        }
    }

    /** Legacy upvote/downvote path (kept for manually published articles). */
    private void legacyVote(CommunityArticle article, String userId, String voteType) {
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
    }
}
