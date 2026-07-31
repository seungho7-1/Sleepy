package com.sleepyproject.sleepy_backend.api.report;

import com.sleepyproject.sleepy_backend.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<?> createReport(@RequestBody Map<String, Object> request, Authentication authentication) {
        String targetType = (String) request.get("targetType");
        Object targetIdObj = request.get("targetId");
        String reason = (String) request.get("reason");

        if (targetType == null || targetIdObj == null || reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "필수 입력값이 누락되었습니다."));
        }

        Long targetId;
        try {
            targetId = targetIdObj instanceof Number ? ((Number) targetIdObj).longValue() : Long.valueOf(targetIdObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "잘못된 대상 ID입니다."));
        }

        Long reportId = reportService.createReport(authentication.getName(), targetType, targetId, reason);
        return ResponseEntity.ok(Map.of(
                "message", "신고가 성공적으로 접수되었습니다.",
                "reportId", reportId
        ));
    }
}
