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
        applicationService.submitApplication(username, request.getSiteUrl(), request.getIntroduction(), request.getShopName(), request.getSnsUrls(), request.getBusinessNumber());
        return ResponseEntity.ok("Application submitted successfully");
    }

    @PostMapping("/verify-business-number")
    public ResponseEntity<?> verifyBusinessNumber(@RequestBody java.util.Map<String, String> body) {
        String businessNumber = body.get("businessNumber");
        if (businessNumber == null || businessNumber.length() != 10) {
            return ResponseEntity.badRequest().body("유효하지 않은 사업자등록번호 형식입니다.");
        }
        
        boolean isValid = applicationService.verifyBusinessNumber(businessNumber);
        return ResponseEntity.ok(java.util.Map.of("isValid", isValid));
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
        private String businessNumber;
    }
}
