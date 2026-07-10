package com.sleepyproject.sleepy_backend.api.admin;

import com.sleepyproject.sleepy_backend.domain.seller.SellerApplication;
import com.sleepyproject.sleepy_backend.service.seller.SellerApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SellerApplicationService applicationService;

    @GetMapping("/applications")
    public ResponseEntity<List<SellerApplication>> getPendingApplications() {
        return ResponseEntity.ok(applicationService.getPendingApplications());
    }

    @PostMapping("/applications/{id}/approve")
    public ResponseEntity<String> approveApplication(@PathVariable Long id) {
        applicationService.approveApplication(id);
        return ResponseEntity.ok("Application approved");
    }

    @PostMapping("/applications/{id}/reject")
    public ResponseEntity<String> rejectApplication(@PathVariable Long id) {
        applicationService.rejectApplication(id);
        return ResponseEntity.ok("Application rejected");
    }
}
