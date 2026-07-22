package com.factshare.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "communityArticles")
public class CommunityArticle {
    @Id private String id;
    private String userId;
    private String type;
    private String title;
    private String content;
    private int credibilityScore;
    private LocalDateTime submissionDate;
    private Vote votes;
    private List<Voter> voters;

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
