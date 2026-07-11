package com.sleepyproject.sleepy_backend.api.admin;

import com.sleepyproject.sleepy_backend.domain.seller.SellerApplication;
import com.sleepyproject.sleepy_backend.service.seller.SellerApplicationService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController("sellerAdminController")
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SellerApplicationService applicationService;

    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationResponse>> getPendingApplications() {
        List<ApplicationResponse> responses = applicationService.getPendingApplications().stream()
                .map(app -> ApplicationResponse.builder()
                        .id(app.getId())
                        .memberId(app.getMember().getId())
                        .siteUrl(app.getSiteUrl())
                        .introduction(app.getIntroduction())
                        .status(app.getStatus().name())
                        .rejectionReason(app.getRejectionReason())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/applications/{id}/approve")
    public ResponseEntity<String> approveApplication(@PathVariable Long id) {
        applicationService.approveApplication(id);
        return ResponseEntity.ok("Application approved");
    }

    @PostMapping("/applications/{id}/reject")
    public ResponseEntity<String> rejectApplication(@PathVariable Long id, @RequestBody RejectionRequest request) {
        applicationService.rejectApplication(id, request.getReason());
        return ResponseEntity.ok("Application rejected");
    }

    @Data
    public static class RejectionRequest {
        private String reason;
    }

    @Data
    @Builder
    public static class ApplicationResponse {
        private Long id;
        private Long memberId;
        private String siteUrl;
        private String introduction;
        private String status;
        private String rejectionReason;
    }
}
