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
    public void signup(SignupRequest request) {
        // 1. 이메일 중복 체크 (DB에 동일한 이메일이 이미 있는지 확인)
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일");
        }

        // 2. 가입할 사용자의 역할(Role) 결정
        // 기본값은 BUYER로 설정하되, 요청에 유효한 역할명(BUYER 또는 SELLER)이 넘어오면 파싱하여 지정함.
        Role assignedRole = Role.BUYER;
        if (request.getRole() != null) {
            try {
                // 대소문자 관계없이 매칭하여 Role Enum으로 변환 시도
                assignedRole = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                // 잘못된 문자열이 넘어오면 기본값인 BUYER를 유지함
                assignedRole = Role.BUYER;
            }
        }

        // 3. Member 엔티티 빌드 및 비밀번호 단방향 암호화(해싱) 진행
        Member member = Member.builder()
                .email(request.getEmail())
                // Spring Security의 BCryptPasswordEncoder를 사용하여 비밀번호를 암호화하여 저장
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(assignedRole) // 앞서 결정된 역할 설정
                .createdAt(LocalDateTime.now()) // 현재 등록 시간 설정
                .build();

        // 4. 데이터베이스에 영속화
        memberRepository.save(member);
    }

    /**
     * 로그인 비즈니스 로직
     *
     * @param loginRequest 이메일 및 비밀번호가 들어있는 로그인 요청 DTO
     * @return 로그인 성공 시 액세스 토큰 및 유저의 정보(이메일, 닉네임, 역할)를 담은 DTO 반환
     * @throws IllegalArgumentException 가입되지 않은 이메일이거나 비밀번호가 불일치할 경우 발생
     */
    public LoginResponse login(LoginRequest loginRequest) {
        // 1. 입력받은 이메일로 데이터베이스에서 회원 조회
        Member member = memberRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저"));

        // 2. 입력받은 평문 비밀번호와 DB에 저장된 암호화된 비밀번호를 비교 검증
        if (!passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호 불일치");
        }

        // 3. 인증 성공 시, 사용자의 이메일과 권한(Role)을 기반으로 JWT 액세스 토큰 발행
        String token = jwtUtil.generateToken(member.getEmail(), member.getRole().name());

        // 4. 로그인 성공 응답용 DTO를 빌드하여 반환
        return LoginResponse.builder()
                .memberId(member.getId()) // 회원 고유 ID 포함 (프론트 소유권 판별용)
                .accessToken(token)
                .email(member.getEmail())
                .nickname(member.getNickname())
                .role(member.getRole())
                .build();
    }

    /**
     * 마이페이지 - 내 프로필 정보 조회
     *
     * @param email JWT에서 추출된 현재 로그인 유저의 이메일
     * @return 닉네임, 이메일, 역할, 가입일시 정보를 담은 MemberInfo DTO
     */
    public MemberInfo getMyInfo(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // LocalDateTime을 읽기 쉬운 문자열 형식으로 포맷하여 전달
        String formattedDate = member.getCreatedAt() != null
                ? member.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                : "-";

        return new MemberInfo(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole().name(),
                formattedDate
        );
    }

    /**
     * 마이페이지 - 내가 등록한 상품 목록 조회 (SELLER 전용)
     *
     * @param email JWT에서 추출된 현재 로그인 유저의 이메일
     * @return 해당 판매자가 등록한 상품 목록 (ProductResponse 리스트)
     */
    public List<ProductResponse> getMyProducts(String email) {
        Member member = memberRepository.findByEmail(email)
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
     * @param email   JWT에서 추출된 현재 로그인 유저의 이메일
     * @param request 변경할 새 닉네임이 담긴 DTO
     */
    @Transactional
    public void updateNickname(String email, NicknameUpdateRequest request) {
        if (request.getNickname() == null || request.getNickname().isBlank()) {
            throw new IllegalArgumentException("닉네임은 빈 값으로 변경할 수 없습니다.");
        }

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 더티 체킹: @Transactional 범위 안에서 엔티티 값 변경 시 별도 save() 없이 DB 자동 반영
        member.updateNickname(request.getNickname());
    }
}