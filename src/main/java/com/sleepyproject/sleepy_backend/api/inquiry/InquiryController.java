package com.sleepyproject.sleepy_backend.api.inquiry;

import com.sleepyproject.sleepy_backend.api.inquiry.dto.InquiryDto;
import com.sleepyproject.sleepy_backend.service.inquiry.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    // 1:1 문의 등록
    @PostMapping
    public ResponseEntity<InquiryDto.Response> createInquiry(
            @RequestBody InquiryDto.Request request,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(inquiryService.createInquiry(username, request));
    }

    //마이페이지에서
    @GetMapping("/me")
    public ResponseEntity<List<InquiryDto.Response>> getMyInquiries(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(inquiryService.getMyInquiries(username));
    }

    // 관리자대시보드에서
    @GetMapping("/admin")
    public ResponseEntity<List<InquiryDto.Response>> getAllInquiries() {
        return ResponseEntity.ok(inquiryService.getAllInquiries());
    }

    // 해당 유저에게 문의 내용보내기
    @PostMapping("/admin/{id}/reply")
    public ResponseEntity<Void> replyToInquiry(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String replyContent = payload.get("reply");
        inquiryService.replyToInquiry(id, replyContent);
        return ResponseEntity.ok().build();
    }
}
