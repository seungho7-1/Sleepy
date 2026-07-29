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
        String repName = body.get("repName");
        String startDate = body.get("startDate");

        if (businessNumber == null || businessNumber.length() != 10) {
            return ResponseEntity.badRequest().body("유효하지 않은 사업자등록번호 형식입니다.");
        }
        if (repName == null || repName.trim().isEmpty() || startDate == null || startDate.length() != 8) {
            return ResponseEntity.badRequest().body("대표자 성명과 개업일자(YYYYMMDD 8자리)를 모두 입력해주세요.");
        }
        
        String result = applicationService.verifyBusinessNumber(businessNumber, repName, startDate);
        
        if ("SUCCESS".equals(result)) {
            return ResponseEntity.ok(java.util.Map.of("isValid", true, "message", "정상 영업중인 사업자로 확인되었습니다."));
        } else if ("INVALID_INFO".equals(result)) {
            return ResponseEntity.ok(java.util.Map.of("isValid", false, "message", "입력하신 대표자 성명 또는 개업일자가 국세청에 등록된 사업자 정보와 일치하지 않습니다."));
        } else if ("CLOSED_BUSINESS".equals(result)) {
            return ResponseEntity.ok(java.util.Map.of("isValid", false, "message", "폐업 또는 휴업한 사업자는 판매자로 신청할 수 없습니다. 운영자에게 이메일(help@sleepy.com)로 문의하세요."));
        } else {
            return ResponseEntity.ok(java.util.Map.of("isValid", false, "message", "국세청 서버 연동 중 오류가 발생했습니다. 다시 시도해주세요."));
        }
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
