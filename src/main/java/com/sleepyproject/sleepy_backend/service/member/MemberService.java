package com.sleepyproject.sleepy_backend.service.member;

import com.sleepyproject.sleepy_backend.api.member.dto.*;
import com.sleepyproject.sleepy_backend.api.product.dto.ProductResponse;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.member.Role;
import com.sleepyproject.sleepy_backend.domain.product.Product;
import com.sleepyproject.sleepy_backend.domain.redis.RefreshToken;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.product.ProductRepository;
import com.sleepyproject.sleepy_backend.repository.product.ProductTagRepository;
import com.sleepyproject.sleepy_backend.domain.member.BrandScrap;
import com.sleepyproject.sleepy_backend.domain.member.BrandScrapRepository;
import com.sleepyproject.sleepy_backend.repository.redis.BlackListedTokenRepository;
import com.sleepyproject.sleepy_backend.repository.redis.RefreshTokenRepository;
import com.sleepyproject.sleepy_backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final ProductTagRepository productTagRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final com.sleepyproject.sleepy_backend.service.MailService mailService;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.sleepyproject.sleepy_backend.repository.NotificationRepository notificationRepository;
    private final com.sleepyproject.sleepy_backend.repository.review.ReviewRepository reviewRepository;
    private final BrandScrapRepository brandScrapRepository;
    private final BlackListedTokenRepository blackListedTokenRepository;
    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private com.sleepyproject.sleepy_backend.service.product.ProductService productService;

    /**
     * 회원가입 비즈니스 로직
     *
     * @param request 회원가입 요청 DTO (이메일, 비밀번호, 닉네임, 역할 정보 포함)
     * @throws IllegalArgumentException 이미 동일한 이메일이 등록되어 있을 경우 발생
     */
    @Value("${spring.security.oauth2.client.registration.naver.client-id:}")
    private String naverClientId;
    @Value("${spring.security.oauth2.client.registration.naver.client-secret:}")
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

        String accessToken = jwtUtil.generateAccessToken(member.getUsername(), member.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(member.getUsername());
        // Redis에 Refresh Token 저장 (비즈니스 로직 완료!)
        refreshTokenRepository.save(new RefreshToken(member.getUsername(), refreshToken));

        return LoginResponse.builder()
                .memberId(member.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(member.getUsername())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .role(member.getRole())
                .build();
    }

    //토큰 재발급 (RTR 적용)
    public LoginResponse refreshAccessToken(String refreshToken){
        // 1. refreshToken으로 유저명, 이메일 추출 및 signature 검증 로직
        String username = jwtUtil.validateAndGetEmailFromRefreshToken(refreshToken);
        //2. redis에 저장된 refreshToken이 맞는지 확인
        com.sleepyproject.sleepy_backend.domain.redis.RefreshToken savedToken = refreshTokenRepository.findById(username).orElseThrow(()-> new IllegalArgumentException("만료되었거나 유효하지 않은 세션입니다."));

        if (!savedToken.getRefreshToken().equals(refreshToken)) {
            // 프론트엔드 동시성 이슈(React StrictMode 등)로 인해 이전 토큰으로 중복 요청이 온 경우 오탐 방지!
            if (refreshToken.equals(savedToken.getPreviousRefreshToken())) {
                Member member = memberRepository.findByUsername(username).orElseThrow(()-> new IllegalArgumentException("존재하지 않는 유저입니다."));
                String newAccessToken = jwtUtil.generateAccessToken(member.getUsername(), member.getRole().name());
                // 방금 전 회전된 최신 토큰을 그대로 재반환 (Redis 상태 변경 안함)
                return LoginResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(savedToken.getRefreshToken())
                        .role(member.getRole())
                        .memberId(member.getId())
                        .nickname(member.getNickname())
                        .build();
            }

            // 해커가 훔쳐간 토큰으로 재발급 시도하거나, 유저가 옛날 토큰으로 시도하는 경우!
            refreshTokenRepository.deleteById(username); // 즉시 강제 로그아웃
            throw new IllegalArgumentException("중복 로그인 또는 토큰 탈취 의심!");
        }

        //3. 유저 권한 조회를 확인.
        Member member = memberRepository.findByUsername(username).orElseThrow(()-> new IllegalArgumentException("존재하지 않는 유저입니다."));

        //4. 새로운 accessToken 및 refreshToken 발급 (RTR 적용)
        String newAccessToken = jwtUtil.generateAccessToken(member.getUsername(), member.getRole().name());
        String newRefreshToken = jwtUtil.generateRefreshToken(member.getUsername());
        
        //5. 새로운 refreshToken을 Redis에 덮어쓰기 저장 (기존 토큰을 previousRefreshToken으로 백업)
        refreshTokenRepository.save(new com.sleepyproject.sleepy_backend.domain.redis.RefreshToken(member.getUsername(), newRefreshToken, refreshToken));

        //6. LoginResponse에 담아서 반환
        return LoginResponse.builder()
                .memberId(member.getId())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(member.getUsername())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .role(member.getRole())
                .build();
    }

    public void logout(String accessToken, String username){
        // 남은 수명만큼 Redis 블랙리스트에 저장
        blackListedTokenRepository.save(new com.sleepyproject.sleepy_backend.domain.redis.BlackListedToken(accessToken, username));
        // Redis에서 기존 Refresh Token 파괴
        refreshTokenRepository.deleteById(username);
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
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 판매자의 공개 프로필 조회
     */
    public java.util.Map<String, Object> getSellerProfile(Long id, String username) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));

        long scrapCount = brandScrapRepository.countBySellerId(id);
        boolean isScrapped = false;
        if (username != null) {
            Member currentUser = memberRepository.findByUsername(username).orElse(null);
            if (currentUser != null) {
                isScrapped = brandScrapRepository.existsByMemberIdAndSellerId(currentUser.getId(), id);
            }
        }

        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", member.getId());
        map.put("shopName", member.getShopName() != null ? member.getShopName() : member.getNickname());
        map.put("profileImageUrl", member.getProfileImageUrl() != null ? member.getProfileImageUrl() : "");
        map.put("introduction", member.getIntroduction() != null ? member.getIntroduction() : "감각적인 디자인과 트렌드를 선도하는 " + (member.getShopName() != null ? member.getShopName() : member.getNickname()) + "입니다.");
        map.put("siteUrl", member.getSiteUrl() != null ? member.getSiteUrl() : "");
        map.put("youtubeUrl", member.getYoutubeUrl() != null ? member.getYoutubeUrl() : "");
        map.put("instagramUrl", member.getInstagramUrl() != null ? member.getInstagramUrl() : "");
        map.put("facebookUrl", member.getFacebookUrl() != null ? member.getFacebookUrl() : "");
        map.put("tiktokUrl", member.getTiktokUrl() != null ? member.getTiktokUrl() : "");
        map.put("scrapCount", scrapCount);
        map.put("isScrapped", isScrapped);
        return map;
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

        // 1. 등록한 상품 삭제 (연관된 태그, 위시리스트, 리뷰 함께 삭제됨)
        List<com.sleepyproject.sleepy_backend.domain.product.Product> products = entityManager.createQuery("SELECT p FROM Product p WHERE p.seller = :member", com.sleepyproject.sleepy_backend.domain.product.Product.class)
                .setParameter("member", member).getResultList();
        for(com.sleepyproject.sleepy_backend.domain.product.Product p : products) {
            // 상품에 달린 리뷰들의 댓글들 먼저 안전하게 삭제 (JPA Cascade 활용)
            List<com.sleepyproject.sleepy_backend.domain.review.Review> reviews = entityManager.createQuery("SELECT r FROM Review r WHERE r.product = :product", com.sleepyproject.sleepy_backend.domain.review.Review.class)
                    .setParameter("product", p).getResultList();
            for(com.sleepyproject.sleepy_backend.domain.review.Review r : reviews) {
                List<com.sleepyproject.sleepy_backend.domain.board.Comment> rComments = entityManager.createQuery("SELECT c FROM Comment c WHERE c.review = :review AND c.parent IS NULL", com.sleepyproject.sleepy_backend.domain.board.Comment.class)
                        .setParameter("review", r).getResultList();
                for(com.sleepyproject.sleepy_backend.domain.board.Comment c : rComments) {
                    entityManager.remove(c);
                }
            }
            entityManager.flush(); // 즉시 DB 반영하여 이후 productService 내의 벌크 쿼리 시 무결성 에러 방지
            productService.delete(p.getId(), member.getUsername());
        }

        // 2. 작성한 게시글 관련 (댓글, 좋아요 등) 삭제
        List<com.sleepyproject.sleepy_backend.domain.board.Post> posts = entityManager.createQuery("SELECT p FROM Post p WHERE p.member = :member", com.sleepyproject.sleepy_backend.domain.board.Post.class)
                .setParameter("member", member)
                .getResultList();
        for(com.sleepyproject.sleepy_backend.domain.board.Post p : posts) {
            // 게시글에 달린 최상위 댓글 조회 후 JPA remove()로 삭제 -> 대댓글까지 자동 cascade 삭제됨
            List<com.sleepyproject.sleepy_backend.domain.board.Comment> topComments = entityManager.createQuery("SELECT c FROM Comment c WHERE c.post = :post AND c.parent IS NULL", com.sleepyproject.sleepy_backend.domain.board.Comment.class)
                    .setParameter("post", p).getResultList();
            for(com.sleepyproject.sleepy_backend.domain.board.Comment c : topComments) {
                entityManager.remove(c);
            }
            entityManager.flush(); // 즉시 DB 반영하여 포스트 삭제 시 외래키 무결성 에러 방지
            entityManager.createQuery("DELETE FROM PostLike pl WHERE pl.post = :post").setParameter("post", p).executeUpdate();
            entityManager.createQuery("DELETE FROM Post p WHERE p = :post").setParameter("post", p).executeUpdate();
        }

        // 3. 본인이 작성한 댓글 전체 안전 삭제 (대댓글 자동 삭제, 이미 지워진 경우 무시)
        List<com.sleepyproject.sleepy_backend.domain.board.Comment> comments = entityManager.createQuery("SELECT c FROM Comment c WHERE c.member = :member", com.sleepyproject.sleepy_backend.domain.board.Comment.class)
                .setParameter("member", member).getResultList();
        for(com.sleepyproject.sleepy_backend.domain.board.Comment c : comments) {
            com.sleepyproject.sleepy_backend.domain.board.Comment managed = entityManager.find(com.sleepyproject.sleepy_backend.domain.board.Comment.class, c.getId());
            if (managed != null) {
                entityManager.remove(managed);
            }
        }
        entityManager.flush(); // 즉시 DB 반영

        // 4. 외래키 제약조건 방지를 위해 나머지 관련 엔티티 일괄 벌크 삭제
        entityManager.createQuery("DELETE FROM Notification n WHERE n.member = :member").setParameter("member", member).executeUpdate();
        entityManager.createQuery("DELETE FROM Wishlist w WHERE w.member = :member").setParameter("member", member).executeUpdate();
        entityManager.createQuery("DELETE FROM PostLike pl WHERE pl.member = :member").setParameter("member", member).executeUpdate();
        entityManager.createQuery("DELETE FROM Review r WHERE r.member = :member").setParameter("member", member).executeUpdate();
        entityManager.createQuery("DELETE FROM SellerApplication sa WHERE sa.member = :member").setParameter("member", member).executeUpdate();
        entityManager.createQuery("DELETE FROM Likes l WHERE l.member = :member").setParameter("member", member).executeUpdate();
        entityManager.createQuery("DELETE FROM Report r WHERE r.reporter = :member").setParameter("member", member).executeUpdate();

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

    public boolean checkEmailExists(String email) {
        return memberRepository.existsByEmail(email);
    }

    public void sendPasswordResetCode(PasswordResetSendCodeRequest request) {
        Member member = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("아이디가 존재하지 않습니다."));
        if (!request.getEmail().equals(member.getEmail())) {
            throw new IllegalArgumentException("아이디와 이메일이 일치하지 않습니다.");
        }
        
        String authCode = String.format("%06d", new java.util.Random().nextInt(1000000));
        
        redisTemplate.opsForValue().set(
            "PW_RESET:" + request.getEmail(), 
            authCode, 
            java.time.Duration.ofMinutes(3)
        );
        
        mailService.sendAuthCodeEmail(request.getEmail(), authCode);
    }

    public boolean verifyPasswordResetCode(PasswordResetVerifyCodeRequest request) {
        String savedCode = redisTemplate.opsForValue().get("PW_RESET:" + request.getEmail());
        if (savedCode != null && savedCode.equals(request.getCode())) {
            // 인증이 완료되면 5분짜리 비밀번호 변경 허용 토큰으로 변경
            redisTemplate.delete("PW_RESET:" + request.getEmail());
            redisTemplate.opsForValue().set("PW_RESET_AUTH:" + request.getEmail(), "TRUE", java.time.Duration.ofMinutes(5));
            return true;
        }
        return false;
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        String isAuth = redisTemplate.opsForValue().get("PW_RESET_AUTH:" + request.getEmail());
        if (isAuth == null || !isAuth.equals("TRUE")) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았거나 만료되었습니다.");
        }
        
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
                
        member.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        redisTemplate.delete("PW_RESET_AUTH:" + request.getEmail());
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getScrappedBrands(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return brandScrapRepository.findByMemberIdOrderByCreatedAtDesc(member.getId(), org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .map(scrap -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", scrap.getSeller().getId());
                    map.put("shopName", scrap.getSeller().getShopName() != null ? scrap.getSeller().getShopName() : scrap.getSeller().getNickname());
                    map.put("profileImageUrl", scrap.getSeller().getProfileImageUrl() != null ? scrap.getSeller().getProfileImageUrl() : "");
                    map.put("scrapCount", brandScrapRepository.countBySellerId(scrap.getSeller().getId()));
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public java.util.Map<String, Object> toggleBrandScrap(String username, Long sellerId) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Member seller = memberRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));

        if (seller.getRole() != Role.SELLER) {
            throw new IllegalArgumentException("해당 유저는 판매자가 아닙니다.");
        }

        java.util.Optional<BrandScrap> existingScrap = brandScrapRepository.findByMemberIdAndSellerId(member.getId(), sellerId);
        boolean isScrapped;

        if (existingScrap.isPresent()) {
            brandScrapRepository.delete(existingScrap.get());
            isScrapped = false;
        } else {
            BrandScrap newScrap = BrandScrap.builder()
                    .member(member)
                    .seller(seller)
                    .createdAt(LocalDateTime.now())
                    .build();
            brandScrapRepository.save(newScrap);
            isScrapped = true;
        }

        return java.util.Map.of(
                "isScrapped", isScrapped,
                "scrapCount", brandScrapRepository.countBySellerId(sellerId)
        );
    }

    @Transactional
    public void updateSellerProfileFields(String username, String shopName, String siteUrl, String introduction, String youtubeUrl, String instagramUrl, String facebookUrl, String tiktokUrl) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (member.getRole() != Role.SELLER) {
            throw new IllegalArgumentException("판매자만 프로필을 수정할 수 있습니다.");
        }

        member.updateSellerProfile(shopName, siteUrl, introduction, youtubeUrl, instagramUrl, facebookUrl, tiktokUrl);
        memberRepository.save(member);
    }
}