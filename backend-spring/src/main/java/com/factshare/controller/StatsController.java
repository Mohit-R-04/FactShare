package com.factshare.controller;
import com.factshare.dto.DashboardStatsResponse;
import com.factshare.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
public class StatsController {
    private final StatsService statsService;
    public StatsController(StatsService statsService) { this.statsService = statsService; }

    @GetMapping("/stats/user")
    public ResponseEntity<DashboardStatsResponse> getUserStats(Authentication auth) {
        return ResponseEntity.ok(statsService.getUserStats(auth.getName()));
    }
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, String>> dashboard(Authentication auth) {
        Map<String, String> resp = new HashMap<>();
        resp.put("status", "success");
        resp.put("message", "Welcome!");
        resp.put("userId", auth.getName());
        return ResponseEntity.ok(resp);
    }
}
