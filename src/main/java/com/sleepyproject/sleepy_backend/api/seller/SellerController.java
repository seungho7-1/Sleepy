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
        
        String email = authentication.getName();
        applicationService.submitApplication(email, request.getSiteUrl(), request.getIntroduction());
        return ResponseEntity.ok("Application submitted successfully");
    }

    @Data
    public static class ApplicationRequest {
        private String siteUrl;
        private String introduction;
    }
}
