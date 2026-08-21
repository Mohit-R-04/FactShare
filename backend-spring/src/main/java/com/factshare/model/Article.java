package com.factshare.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String userId;
    private String type;
    @Column(length = 500)
    private String title;
    @Column(length = 10000)
    private String content;
    private int credibilityScore = 50;
    @Column(nullable = false)
    private LocalDateTime submissionDate = LocalDateTime.now();

    public Article() {}
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
}
