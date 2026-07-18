package com.sleepyproject.sleepy_backend.api.admin;

import com.sleepyproject.sleepy_backend.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        return ResponseEntity.ok(adminService.getOldDashboardStats());
    }

    // Report Management
    @GetMapping("/reports")
    public ResponseEntity<?> getReports() {
        return ResponseEntity.ok(adminService.getPendingReports());
    }

    @PostMapping("/reports/{id}/resolve")
    public ResponseEntity<?> resolveReport(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String action = request.get("action"); // BLIND, SUSPEND_USER, NONE
        adminService.resolveReport(id, action);
        return ResponseEntity.ok(Map.of("message", "신고가 처리되었습니다."));
    }

    // User / Seller Management
    @GetMapping("/members")
    public ResponseEntity<?> getMembers() {
        return ResponseEntity.ok(adminService.getAllMembers());
    }

    @PostMapping("/members/{id}/suspend")
    public ResponseEntity<?> suspendMember(@PathVariable Long id) {
        adminService.suspendMember(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/members/{id}/unsuspend")
    public ResponseEntity<?> unsuspendMember(@PathVariable Long id) {
        adminService.unsuspendMember(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sellers/applications")
    public ResponseEntity<?> getSellerApplications() {
        return ResponseEntity.ok(adminService.getPendingSellerApplications());
    }

    @PostMapping("/sellers/applications/{id}/approve")
    public ResponseEntity<?> approveSeller(@PathVariable Long id) {
        adminService.approveSellerApplication(id);
        return ResponseEntity.ok(Map.of("message", "셀러 권한이 승인되었습니다."));
    }

    @PostMapping("/sellers/applications/{id}/reject")
    public ResponseEntity<?> rejectSeller(@PathVariable Long id, @RequestBody Map<String, String> request) {
        adminService.rejectSellerApplication(id, request.get("reason"));
        return ResponseEntity.ok(Map.of("message", "셀러 신청이 반려되었습니다."));
    }

    // Product Management
    @GetMapping("/products")
    public ResponseEntity<?> getProducts() {
        return ResponseEntity.ok(adminService.getAllProducts());
    }

    @PostMapping("/products/{id}/hide")
    public ResponseEntity<?> hideProduct(@PathVariable Long id) {
        adminService.hideProduct(id);
        return ResponseEntity.ok(Map.of("message", "상품이 숨김 처리되었습니다."));
    }

    @PostMapping("/products/{id}/unhide")
    public ResponseEntity<?> unhideProduct(@PathVariable Long id) {
        adminService.unhideProduct(id);
        return ResponseEntity.ok(Map.of("message", "상품 숨김이 해제되었습니다."));
    }
}
