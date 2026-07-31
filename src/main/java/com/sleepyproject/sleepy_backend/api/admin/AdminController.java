package com.sleepyproject.sleepy_backend.api.admin;

import com.sleepyproject.sleepy_backend.service.admin.AdminService;
import com.sleepyproject.sleepy_backend.service.inquiry.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final InquiryService inquiryService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // 관리자 대시보드 조회
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        return ResponseEntity.ok(adminService.getOldDashboardStats());
    }

    // 신고 내역 조회
    @GetMapping("/reports")
    public ResponseEntity<?> getReports() {
        return ResponseEntity.ok(adminService.getPendingReports());
    }

    // 해당 신고 처리
    @PostMapping("/reports/{id}/resolve")
    public ResponseEntity<?> resolveReport(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String action = request.get("action"); // BLIND, SUSPEND_USER, NONE
        adminService.resolveReport(id, action);
        return ResponseEntity.ok(Map.of("message", "신고가 처리되었습니다."));
    }

    // 회원 목록 조회
    @GetMapping("/members")
    public ResponseEntity<?> getMembers() {
        return ResponseEntity.ok(adminService.getAllMembers());
    }

    // 회원 정지
    @PostMapping("/members/{id}/suspend")
    public ResponseEntity<?> suspendMember(@PathVariable Long id) {
        adminService.suspendMember(id);
        return ResponseEntity.ok().build();
    }

    // 회원 정지 해제
    @PostMapping("/members/{id}/unsuspend")
    public ResponseEntity<?> unsuspendMember(@PathVariable Long id) {
        adminService.unsuspendMember(id);
        return ResponseEntity.ok().build();
    }

    // 판매자 신청 리스트
    @GetMapping("/sellers/applications")
    public ResponseEntity<?> getSellerApplications() {
        return ResponseEntity.ok(adminService.getPendingSellerApplications());
    }

    // 셀러 신청 승인
    @PostMapping("/sellers/applications/{id}/approve")
    public ResponseEntity<?> approveSeller(@PathVariable Long id) {
        adminService.approveSellerApplication(id);
        return ResponseEntity.ok(Map.of("message", "셀러 권한이 승인되었습니다."));
    }

    // 셀러 신청 거절 요청
    @PostMapping("/sellers/applications/{id}/reject")
    public ResponseEntity<?> rejectSeller(@PathVariable Long id, @RequestBody Map<String, String> request) {
        adminService.rejectSellerApplication(id, request.get("reason"));
        return ResponseEntity.ok(Map.of("message", "셀러 신청이 반려되었습니다."));
    }

    // 상품 관리
    @GetMapping("/products")
    public ResponseEntity<?> getProducts() {
        return ResponseEntity.ok(adminService.getAllProducts());
    }

    //상품 숨기기
    @PostMapping("/products/{id}/hide")
    public ResponseEntity<?> hideProduct(@PathVariable Long id) {
        adminService.hideProduct(id);
        return ResponseEntity.ok(Map.of("message", "상품이 숨김 처리되었습니다."));
    }

    //상품 숨기기 취소
    @PostMapping("/products/{id}/unhide")
    public ResponseEntity<?> unhideProduct(@PathVariable Long id) {
        adminService.unhideProduct(id);
        return ResponseEntity.ok(Map.of("message", "상품 숨김이 해제되었습니다."));
    }

    // 1:1 문의 관리 리스트
    @GetMapping("/inquiries")
    public ResponseEntity<?> getInquiries() {
        return ResponseEntity.ok(inquiryService.getAllInquiries());
    }

    // 1:1 문의 답변하기
    @PostMapping("/inquiries/{id}/reply")
    public ResponseEntity<?> replyToInquiry(@PathVariable Long id, @RequestBody Map<String, String> request) {
        inquiryService.replyToInquiry(id, request.get("reply"));
        return ResponseEntity.ok(Map.of("message", "문의 답변이 등록되었습니다."));
    }
}
