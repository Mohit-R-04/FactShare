package com.factshare.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One community vote row. The (article_id, user_id) unique constraint makes
 * "one vote per reviewer" a database-enforced invariant; changing a vote is an
 * UPDATE and un-voting is a DELETE instead of a whole-document read-modify-write.
 */
@Entity
@Table(name = "community_votes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_community_votes_article_user", columnNames = {"article_id", "user_id"})
})
public class CommunityVote {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(name = "article_id", nullable = false)
    private String articleId;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "vote_type", nullable = false)
    private String voteType;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public CommunityVote() {}

    public CommunityVote(String articleId, String userId, String voteType) {
        this.articleId = articleId;
        this.userId = userId;
        this.voteType = voteType;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getArticleId() { return articleId; }
    public void setArticleId(String v) { this.articleId = v; }
    public String getUserId() { return userId; }
    public void setUserId(String v) { this.userId = v; }
    public String getVoteType() { return voteType; }
    public void setVoteType(String v) { this.voteType = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
