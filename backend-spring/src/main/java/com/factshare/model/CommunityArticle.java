package com.factshare.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community_articles")
public class CommunityArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String userId;
    private String type;
    @Column(length = 500)
    private String title;
    @Column(length = 10000)
    private String content;
    private int credibilityScore;
    @Column(nullable = false)
    private LocalDateTime submissionDate;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "upvotes", column = @Column(name = "upvotes")),
        @AttributeOverride(name = "downvotes", column = @Column(name = "downvotes"))
    })
    private Vote votes;

    // Review-feed fields (auto-published verification results)
    private String category = "Other";
    private String reviewStatus = "OPEN"; // OPEN | NEEDS_REVIEW | REVIEWED
    private String verdict;
    @Column(length = 2000)
    private String claim;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "trueVotes", column = @Column(name = "true_votes")),
        @AttributeOverride(name = "falseVotes", column = @Column(name = "false_votes")),
        @AttributeOverride(name = "uncertainVotes", column = @Column(name = "uncertain_votes"))
    })
    private ReviewVotes communityVotes;
    private double communityConfidence;
    private int aiScore;
    private int disputeCount;

    /**
     * Not persisted; assembled by CommunityService from the community_votes
     * table so the API keeps returning the historical embedded shape.
     */
    @Transient
    private List<Voter> voters = new ArrayList<>();

    public CommunityArticle() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String v) { this.userId = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public int getCredibilityScore() { return credibilityScore; }
    public void setCredibilityScore(int v) { this.credibilityScore = v; }
    public LocalDateTime getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(LocalDateTime v) { this.submissionDate = v; }
    public Vote getVotes() { return votes; }
    public void setVotes(Vote v) { this.votes = v; }
    public List<Voter> getVoters() { return voters; }
    public void setVoters(List<Voter> v) { this.voters = v; }

    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String v) { this.reviewStatus = v; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String v) { this.verdict = v; }
    public String getClaim() { return claim; }
    public void setClaim(String v) { this.claim = v; }
    public ReviewVotes getCommunityVotes() { return communityVotes; }
    public void setCommunityVotes(ReviewVotes v) { this.communityVotes = v; }
    public double getCommunityConfidence() { return communityConfidence; }
    public void setCommunityConfidence(double v) { this.communityConfidence = v; }
    public int getAiScore() { return aiScore; }
    public void setAiScore(int v) { this.aiScore = v; }
    public int getDisputeCount() { return disputeCount; }
    public void setDisputeCount(int v) { this.disputeCount = v; }

    @Embeddable
    public static class Vote {
        private int upvotes;
        private int downvotes;
        public Vote() {}
        public Vote(int upvotes, int downvotes) { this.upvotes = upvotes; this.downvotes = downvotes; }
        public int getUpvotes() { return upvotes; }
        public void setUpvotes(int v) { this.upvotes = v; }
        public int getDownvotes() { return downvotes; }
        public void setDownvotes(int v) { this.downvotes = v; }
    }

    /** Community review votes: True / False / Uncertain. */
    @Embeddable
    public static class ReviewVotes {
        private int trueVotes;
        private int falseVotes;
        private int uncertainVotes;
        public ReviewVotes() {}
        public ReviewVotes(int trueVotes, int falseVotes, int uncertainVotes) {
            this.trueVotes = trueVotes; this.falseVotes = falseVotes; this.uncertainVotes = uncertainVotes;
        }
        public int getTrueVotes() { return trueVotes; }
        public void setTrueVotes(int v) { this.trueVotes = v; }
        public int getFalseVotes() { return falseVotes; }
        public void setFalseVotes(int v) { this.falseVotes = v; }
        public int getUncertainVotes() { return uncertainVotes; }
        public void setUncertainVotes(int v) { this.uncertainVotes = v; }
        public int total() { return trueVotes + falseVotes + uncertainVotes; }
    }

    public static class Voter {
        private String userId;
        private String voteType;
        public Voter() {}
        public Voter(String userId, String voteType) { this.userId = userId; this.voteType = voteType; }
        public String getUserId() { return userId; }
        public void setUserId(String v) { this.userId = v; }
        public String getVoteType() { return voteType; }
        public void setVoteType(String v) { this.voteType = v; }
    }
}
