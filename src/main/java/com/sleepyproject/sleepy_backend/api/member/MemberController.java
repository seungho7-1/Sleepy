package com.sleepyproject.sleepy_backend.api.member;

import com.sleepyproject.sleepy_backend.api.member.dto.*;
import com.sleepyproject.sleepy_backend.api.product.dto.ProductResponse;
import com.sleepyproject.sleepy_backend.service.member.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        memberService.signup(request);
        return ResponseEntity.ok(Map.of("message", "회원가입 성공"));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse loginResult = memberService.login(request);
        return ResponseEntity.ok(loginResult);
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return ResponseEntity.ok(Map.of("message", "로그아웃 성공"));
    }

    /**
     * 마이페이지 - 내 프로필 정보 조회
     * GET /api/auth/me
     * JWT 토큰 인증 필요
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("unauthorized");
        }
        String email = (String) authentication.getPrincipal();
        MemberInfo memberInfo = memberService.getMyInfo(email);
        return ResponseEntity.ok(memberInfo);
    }

    /**
     * 마이페이지 - 내가 등록한 상품 목록 조회 (SELLER 전용)
     * GET /api/auth/my-products
     * JWT 토큰 인증 필요
     */
    @GetMapping("/my-products")
    public ResponseEntity<?> myProducts(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("unauthorized");
        }
        String email = (String) authentication.getPrincipal();
        List<ProductResponse> products = memberService.getMyProducts(email);
        return ResponseEntity.ok(products);
    }

    /**
     * 마이페이지 - 닉네임 수정
     * PATCH /api/auth/nickname
     * JWT 토큰 인증 필요
     */
    @PatchMapping("/nickname")
    public ResponseEntity<?> updateNickname(
            @RequestBody NicknameUpdateRequest request,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("unauthorized");
        }
        String email = (String) authentication.getPrincipal();
        memberService.updateNickname(email, request);
        return ResponseEntity.ok(Map.of("message", "닉네임이 변경되었습니다."));
    }
}
