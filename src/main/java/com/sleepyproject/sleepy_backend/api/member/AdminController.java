package com.sleepyproject.sleepy_backend.api.member;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 관리자(ADMIN) 권한 페이지에 필요한 통계 및 회원 상태 조회를 처리하는 컨트롤러 클래스입니다.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    /**
     * 관리자 대시보드 통계 자료를 조회합니다. (회원 수, 등록된 상품 수, 전체 회원 목록)
     *
     * @return 대시보드 통계 정보 Map
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        long totalMembers = memberRepository.count();
        long totalProducts = productRepository.count();

        List<Map<String, Object>> members = memberRepository.findAll().stream()
                .map(m -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", m.getId());
                    map.put("email", m.getEmail());
                    map.put("nickname", m.getNickname());
                    map.put("role", m.getRole().name());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "totalMembers", totalMembers,
                "totalProducts", totalProducts,
                "members", members
        ));
    }
}
