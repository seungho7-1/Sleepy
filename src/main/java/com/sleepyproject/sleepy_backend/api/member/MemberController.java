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
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * 회원가입, 로그인, 마이페이지 정보 관리 등의 인증 관련 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final com.sleepyproject.sleepy_backend.service.seller.SellerApplicationService sellerApplicationService;
    private final com.sleepyproject.sleepy_backend.repository.member.MemberRepository memberRepository;
    private final JdbcTemplate jdbcTemplate;

    @org.springframework.beans.factory.annotation.Value("${spring.security.oauth2.client.registration.kakao.client-id:}")
    private String kakaoId;
    @org.springframework.beans.factory.annotation.Value("${spring.security.oauth2.client.registration.kakao.client-secret:}")
    private String kakaoSecret;
    @org.springframework.beans.factory.annotation.Value("${spring.security.oauth2.client.registration.naver.client-id:}")
    private String naverId;
    @org.springframework.beans.factory.annotation.Value("${spring.security.oauth2.client.registration.naver.client-secret:}")
    private String naverSecret;

    @GetMapping("/debug-secrets")
    public ResponseEntity<?> debugSecrets(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }
        return ResponseEntity.ok(Map.of(
            "kakaoId", mask(kakaoId),
            "kakaoSecret", mask(kakaoSecret),
            "naverId", mask(naverId),
            "naverSecret", mask(naverSecret)
        ));
    }

    private String mask(String val) {
        if (val == null || val.isEmpty()) return "empty";
        if (val.startsWith("YOUR_")) return "DUMMY: " + val;
        return val.substring(0, Math.min(val.length(), 4)) + "... (len: " + val.length() + ")";
    }

    /**
     * 신규 회원가입을 처리합니다. (비로그인 허용)
     *
     * @param request 회원가입 정보 DTO (이메일, 비밀번호, 닉네임, 권한 등)
     * @return 성공 메시지
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        memberService.signup(request);
        return ResponseEntity.ok(Map.of("message", "회원가입 성공"));
    }

    /**
     * 로그인 인증을 처리하고 토큰을 반환합니다. (비로그인 허용)
     *
     * @param request 로그인 정보 DTO (이메일, 비밀번호)
     * @return 로그인 성공 결과 DTO (JWT 토큰 및 사용자 정보 포함)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse loginResult = memberService.login(request);
        return ResponseEntity.ok(loginResult);
    }

    /**
     * 로그아웃을 처리하고 세션을 무효화합니다.
     *
     * @param request        HTTP 서블릿 요청
     * @param response       HTTP 서블릿 응답
     * @param authentication 현재 로그인된 유저 인증 정보
     * @return 성공 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return ResponseEntity.ok(Map.of("message", "로그아웃 성공"));
    }

    /**
     * 마이페이지에서 내 프로필 정보를 조회합니다. (인증 필요)
     *
     * @param authentication 현재 로그인된 유저 인증 정보
     * @return 로그인된 유저 프로필 정보 DTO
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        MemberInfo memberInfo = memberService.getMyInfo(username);
        return ResponseEntity.ok(memberInfo);
    }

    /**
     * 마이페이지에서 내가 등록한 상품 목록을 조회합니다. (판매자 권한 전용)
     *
     * @param authentication 현재 로그인된 판매자 인증 정보
     * @return 내가 판매 등록한 상품 목록 리스트
     */
    @GetMapping("/my-products")
    public ResponseEntity<?> myProducts(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        List<ProductResponse> products = memberService.getMyProducts(username);
        return ResponseEntity.ok(products);
    }

    /**
     * 마이페이지에서 닉네임을 변경합니다. (인증 필요)
     *
     * @param request        변경할 닉네임 정보 DTO
     * @param authentication 현재 로그인된 유저 인증 정보
     * @return 성공 메시지
     */
    @PatchMapping("/nickname")
    public ResponseEntity<?> updateNickname(@RequestBody NicknameUpdateRequest request, Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        memberService.updateNickname(username, request);
        return ResponseEntity.ok(Map.of("message", "닉네임이 변경되었습니다."));
    }

    @PatchMapping("/profile-image")
    public ResponseEntity<?> updateProfileImage(@RequestBody Map<String, String> request, Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        memberService.updateProfileImage(username, request.get("profileImageUrl"));
        return ResponseEntity.ok(Map.of("message", "프로필 이미지가 변경되었습니다."));
    }

    @PatchMapping("/email")
    public ResponseEntity<?> updateEmail(@RequestBody Map<String, String> request, Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        memberService.updateEmail(username, request.get("email"));
        return ResponseEntity.ok(Map.of("message", "이메일이 변경되었습니다."));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> request, Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        memberService.updateProfile(username, request.get("nickname"), request.get("profileImageUrl"), request.get("email"));
        return ResponseEntity.ok(Map.of("message", "프로필이 업데이트되었습니다."));
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<?> withdraw(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        memberService.withdrawMember(username);
        return ResponseEntity.ok(Map.of("message", "회원 탈퇴 완료"));
    }

    @PostMapping("/oauth2/onboarding")
    public ResponseEntity<?> onboarding(Authentication authentication, @RequestBody OnboardingRequest request) {
        String username = (String) authentication.getPrincipal();
        
        memberService.updateNickname(username, new NicknameUpdateRequest(request.getNickname()));
        
        if ("SELLER".equalsIgnoreCase(request.getRole())) {
            sellerApplicationService.submitApplication(username, request.getSiteUrl(), request.getIntroduction(), request.getShopName(), request.getSnsUrls());
        }
        
        com.sleepyproject.sleepy_backend.domain.member.Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        member.completeOnboarding();
        memberRepository.save(member);
        
        return ResponseEntity.ok(Map.of(
            "message", "온보딩 완료",
            "nickname", member.getNickname(),
            "role", member.getRole().name()
        ));
    }

    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam("username") String username) {
        boolean exists = memberService.checkUsernameExists(username);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<?> checkNickname(@RequestParam("nickname") String nickname) {
        boolean exists = memberService.checkNicknameExists(nickname);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam("email") String email) {
        boolean exists = memberService.checkEmailExists(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/reset-db")
    public ResponseEntity<?> resetDb(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }
        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            jdbcTemplate.execute("TRUNCATE TABLE board_comment");
            jdbcTemplate.execute("TRUNCATE TABLE board_post");
            jdbcTemplate.execute("TRUNCATE TABLE product_tag");
            jdbcTemplate.execute("TRUNCATE TABLE tag");
            jdbcTemplate.execute("TRUNCATE TABLE product");
            jdbcTemplate.execute("TRUNCATE TABLE seller_application");
            jdbcTemplate.execute("TRUNCATE TABLE member");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            return ResponseEntity.ok(Map.of("message", "데이터베이스의 모든 데이터가 성공적으로 초기화되었습니다!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/seed-admin")
    public ResponseEntity<?> seedAdmin() {
        boolean adminExists = memberRepository.findAll().stream().anyMatch(m -> m.getRole() == com.sleepyproject.sleepy_backend.domain.member.Role.ADMIN);
        if (adminExists) {
            return ResponseEntity.status(403).body(Map.of("error", "이미 관리자 계정이 존재합니다."));
        }
        try {
            SignupRequest request = new SignupRequest();
            request.setUsername("admin");
            request.setEmail("admin@sleepy.com");
            request.setPassword("admin1234");
            request.setNickname("관리자");
            request.setRole("ADMIN");
            memberService.signup(request);
            
            com.sleepyproject.sleepy_backend.domain.member.Member member = memberRepository.findByUsername("admin")
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            member.completeOnboarding();
            memberRepository.save(member);
            
            return ResponseEntity.ok(Map.of("message", "관리자 계정(admin / admin1234)이 생성되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/password/send-code")
    public ResponseEntity<?> sendPasswordResetCode(@RequestBody PasswordResetSendCodeRequest request) {
        memberService.sendPasswordResetCode(request);
        return ResponseEntity.ok(Map.of("message", "인증 코드가 이메일로 발송되었습니다."));
    }

    @PostMapping("/password/verify-code")
    public ResponseEntity<?> verifyPasswordResetCode(@RequestBody PasswordResetVerifyCodeRequest request) {
        boolean isValid = memberService.verifyPasswordResetCode(request);
        if (isValid) {
            return ResponseEntity.ok(Map.of("message", "인증이 완료되었습니다.", "valid", true));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "인증 코드가 올바르지 않거나 만료되었습니다.", "valid", false));
        }
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest request) {
        memberService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "비밀번호가 성공적으로 변경되었습니다."));
    }

    @lombok.Data
    public static class OnboardingRequest {
        private String nickname;
        private String role;
        private String siteUrl;
        private String introduction;
        private String shopName;
        private String snsUrls;
    }
}
