package com.sleepyproject.sleepy_backend.api.seller;

import com.sleepyproject.sleepy_backend.service.seller.SellerApplicationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerController {

    private final SellerApplicationService applicationService;

    @PostMapping("/apply")
    public ResponseEntity<String> applyForSeller(Authentication authentication, @RequestBody ApplicationRequest request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        String username = authentication.getName();
        applicationService.submitApplication(username, request.getSiteUrl(), request.getIntroduction(), request.getShopName(), request.getSnsUrls());
        return ResponseEntity.ok("Application submitted successfully");
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getLatestApplication(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String username = authentication.getName();
        com.sleepyproject.sleepy_backend.domain.seller.SellerApplication app = applicationService.getLatestApplication(username);
        if (app == null) {
            return ResponseEntity.ok(java.util.Collections.emptyMap());
        }
        return ResponseEntity.ok(java.util.Map.of(
            "id", app.getId(),
            "siteUrl", app.getSiteUrl(),
            "introduction", app.getIntroduction(),
            "shopName", app.getShopName(),
            "snsUrls", app.getSnsUrls() != null ? app.getSnsUrls() : "",
            "status", app.getStatus().name(),
            "rejectionReason", app.getRejectionReason() != null ? app.getRejectionReason() : ""
        ));
    }

    @Data
    public static class ApplicationRequest {
        private String siteUrl;
        private String introduction;
        private String shopName;
        private String snsUrls;
    }
}
