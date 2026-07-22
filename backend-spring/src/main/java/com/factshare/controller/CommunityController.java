package com.factshare.controller;
import com.factshare.dto.VoteRequest;
import com.factshare.model.CommunityArticle;
import com.factshare.service.CommunityService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController

public class CommunityController {
    private final CommunityService communityService;
    public CommunityController(CommunityService communityService) { this.communityService = communityService; }
    @GetMapping("/community/articles")
    public ResponseEntity<List<CommunityArticle>> getAll() {
        return ResponseEntity.ok(communityService.getAllArticles());
    }
    @PostMapping("/community/articles")
    public ResponseEntity<CommunityArticle> publish(Authentication auth, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(communityService.publishArticle(auth.getName(), body));
    }
    @PostMapping("/community/articles/{id}/vote")
    public ResponseEntity<CommunityArticle> vote(Authentication auth, @PathVariable String id, @RequestBody VoteRequest req) {
        return ResponseEntity.ok(communityService.vote(auth.getName(), id, req));
    }
}
