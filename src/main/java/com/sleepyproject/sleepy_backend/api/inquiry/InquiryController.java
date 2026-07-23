package com.sleepyproject.sleepy_backend.api.inquiry;

import com.sleepyproject.sleepy_backend.api.dto.InquiryDto;
import com.sleepyproject.sleepy_backend.service.inquiry.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    public ResponseEntity<InquiryDto.Response> createInquiry(
            @RequestBody InquiryDto.Request request,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(inquiryService.createInquiry(username, request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<InquiryDto.Response>> getMyInquiries(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(inquiryService.getMyInquiries(username));
    }

    // 관리자용 엔드포인트
    @GetMapping("/admin")
    public ResponseEntity<List<InquiryDto.Response>> getAllInquiries() {
        return ResponseEntity.ok(inquiryService.getAllInquiries());
    }

    @PostMapping("/admin/{id}/reply")
    public ResponseEntity<Void> replyToInquiry(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> payload) {
        String replyContent = payload.get("reply");
        inquiryService.replyToInquiry(id, replyContent);
        return ResponseEntity.ok().build();
    }
}
