package com.sleepyproject.sleepy_backend.service.member;

import com.sleepyproject.sleepy_backend.api.member.dto.*;
import com.sleepyproject.sleepy_backend.api.product.dto.ProductResponse;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.member.Role;
import com.sleepyproject.sleepy_backend.domain.product.Product;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.product.ProductRepository;
import com.sleepyproject.sleepy_backend.repository.product.ProductTagRepository;
import com.sleepyproject.sleepy_backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 회원 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final ProductTagRepository productTagRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 회원가입 비즈니스 로직
     *
     * @param request 회원가입 요청 DTO (이메일, 비밀번호, 닉네임, 역할 정보 포함)
     * @throws IllegalArgumentException 이미 동일한 이메일이 등록되어 있을 경우 발생
     */
    @org.springframework.beans.factory.annotation.Value("${spring.security.oauth2.client.registration.naver.client-id:}")
    private String naverClientId;
    @org.springframework.beans.factory.annotation.Value("${spring.security.oauth2.client.registration.naver.client-secret:}")
    private String naverClientSecret;

    public void signup(SignupRequest request) {
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 아이디");
        }

        Role assignedRole = Role.BUYER;
        if (request.getRole() != null) {
            try {
                assignedRole = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                assignedRole = Role.BUYER;
            }
        }

        String email = request.getEmail();
        if (email != null && email.trim().isEmpty()) {
            email = null;
        }

        Member member = Member.builder()
                .username(request.getUsername())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(assignedRole)
                .createdAt(LocalDateTime.now())
                .onboarded(true)
                .build();

        memberRepository.save(member);
    }

    public LoginResponse login(LoginRequest loginRequest) {
        Member member = memberRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호 불일치");
        }

        String token = jwtUtil.generateToken(member.getUsername(), member.getRole().name());

        return LoginResponse.builder()
                .memberId(member.getId())
                .accessToken(token)
                .username(member.getUsername())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .role(member.getRole())
                .build();
    }

    public MemberInfo getMyInfo(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        String formattedDate = member.getCreatedAt() != null
                ? member.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                : "-";

        return new MemberInfo(
                member.getId(),
                member.getUsername(),
                member.getEmail(),
                member.getNickname(),
                member.getRole().name(),
                formattedDate,
                member.getOauthProvider(),
                member.getProfileImageUrl()
        );
    }

    /**
     * 마이페이지 - 내가 등록한 상품 목록 조회 (SELLER 전용)
     *
     * @param username JWT에서 추출된 현재 로그인 유저의 아이디
     * @return 해당 판매자가 등록한 상품 목록 (ProductResponse 리스트)
     */
    public List<ProductResponse> getMyProducts(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 해당 판매자 ID 기준으로 등록 상품 목록 조회 (최신순)
        List<Product> products = productRepository.findBySellerIdOrderByIdDesc(member.getId());

        // 엔티티 리스트를 응답 DTO 리스트로 변환하여 반환
        return products.stream()
                .map(p -> {
                    List<String> tags = productTagRepository.findByProduct(p).stream()
                            .map(pt -> pt.getTag().getName())
                            .collect(Collectors.toList());
                    return new ProductResponse(
                            p.getId(),
                            p.getName(),
                            p.getPrice(),
                            p.getDescription(),
                            p.getFirstImageUrl(),
                            p.getShopName(),
                            p.getPurchaseUrl(),
                            p.getSeller().getId(),
                            p.getCapacity(),
                            p.getTexture(),
                            p.getScent(),
                            p.getColor(),
                            p.getReleaseDate(),
                            tags,
                            p.getVideoUrl(),
                            p.getVideoType(),
                            p.getImageUrlList(),
                            p.getDescriptionImageUrlList()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 마이페이지 - 닉네임 수정
     *
     * @param username JWT에서 추출된 현재 로그인 유저의 아이디
     * @param request  변경할 새 닉네임이 담긴 DTO
     */
    @Transactional
    public void updateNickname(String username, NicknameUpdateRequest request) {
        if (request.getNickname() == null || request.getNickname().isBlank()) {
            throw new IllegalArgumentException("닉네임은 빈 값으로 변경할 수 없습니다.");
        }

        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 더티 체킹: @Transactional 범위 안에서 엔티티 값 변경 시 별도 save() 없이 DB 자동 반영
        member.updateNickname(request.getNickname());
    }

    @Transactional
    public void updateProfileImage(String username, String profileImageUrl) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        member.updateProfileImage(profileImageUrl);
    }

    @Transactional
    public void updateEmail(String username, String email) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
        }
        member.updateEmail(email);
    }

    @Transactional
    public void updateProfile(String username, String nickname, String profileImageUrl, String email) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        if (nickname != null && !nickname.isBlank()) {
            member.updateNickname(nickname);
        }
        if (profileImageUrl != null) {
            member.updateProfileImage(profileImageUrl);
        }
        if (email != null && !email.isBlank()) {
            if (!email.equals(member.getEmail()) && memberRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
            }
            member.updateEmail(email);
        }
    }

    /**
     * 회원 탈퇴 및 소셜 연동 해제
     *
     * @param username 탈퇴할 유저의 아이디
     */
    @Transactional
    public void withdrawMember(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        if (member.getOauthProvider() != null) {
            try {
                if ("KAKAO".equalsIgnoreCase(member.getOauthProvider())) {
                    unlinkKakao(member.getOauthAccessToken());
                } else if ("NAVER".equalsIgnoreCase(member.getOauthProvider())) {
                    unlinkNaver(member.getOauthAccessToken());
                }
            } catch (Exception e) {
                System.err.println("OAuth2 unlink failed: " + e.getMessage());
            }
        }

        memberRepository.delete(member);
    }

    private void unlinkKakao(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) return;
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
        restTemplate.postForEntity("https://kapi.kakao.com/v1/user/unlink", entity, String.class);
    }

    private void unlinkNaver(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) return;
        String url = "https://nid.naver.com/oauth2.0/token?grant_type=delete"
                + "&client_id=" + naverClientId
                + "&client_secret=" + naverClientSecret
                + "&access_token=" + accessToken
                + "&service_provider=NAVER";
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        restTemplate.getForObject(url, String.class);
    }

    public boolean checkUsernameExists(String username) {
        return memberRepository.existsByUsername(username);
    }

    public boolean checkNicknameExists(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }
}