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

/**
 * 회원가입, 로그인, 마이페이지 정보 관리 등의 인증 관련 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

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
        String email = (String) authentication.getPrincipal();
        MemberInfo memberInfo = memberService.getMyInfo(email);
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
        String email = (String) authentication.getPrincipal();
        List<ProductResponse> products = memberService.getMyProducts(email);
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
        String email = (String) authentication.getPrincipal();
        memberService.updateNickname(email, request);
        return ResponseEntity.ok(Map.of("message", "닉네임이 변경되었습니다."));
    }
}
