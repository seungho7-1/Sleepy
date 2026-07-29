package com.sleepyproject.sleepy_backend.api.product;

import com.sleepyproject.sleepy_backend.api.product.dto.ProductCreateRequest;
import com.sleepyproject.sleepy_backend.api.product.dto.ProductUpdateRequest;
import com.sleepyproject.sleepy_backend.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 상품 관련 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 * - 상품 등록, 조회, 수정, 삭제
 * - 찜하기 토글 및 조회
 * - 외부 상품 링크 정보 크롤링
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 새로운 상품을 등록합니다. (판매자 권한 전용)
     *
     * @param request        등록할 상품 정보 DTO
     * @param authentication 현재 로그인된 유저 인증 정보
     * @return 등록된 상품 ID
     */
    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody ProductCreateRequest request,
                                    Authentication authentication) {
        Long id = productService.create(request, authentication.getName());
        return ResponseEntity.ok(Map.of("productId", id));
    }

    /**
     * 상품 목록을 조회합니다. (검색 키워드 및 페이징 지원)
     *
     * @param keyword 검색할 키워드 (선택)
     * @param page    조회할 페이지 번호 (기본값: 0)
     * @param size    페이지당 상품 수 (기본값: 10)
     * @return 페이징 처리된 상품 목록
     */
    @GetMapping("/list")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        // sort 파라미터 파싱 (예: "reviewCount,desc")
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0].trim();
        Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1].trim())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        return ResponseEntity.ok(productService.getProducts(category, keyword, sellerId, pageable));
    }

    /**
     * 특정 상품의 상세 정보를 조회합니다.
     *
     * @param id 조회할 상품 ID
     * @return 상품 상세 정보
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductDetail(id));
    }

    /**
     * 등록된 상품 정보를 수정합니다. (판매자 권한 전용)
     *
     * @param id             수정할 상품 ID
     * @param request        수정할 상품 정보 DTO
     * @param authentication 현재 로그인된 유저 인증 정보
     * @return 성공 메시지
     */
    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody ProductUpdateRequest request,
                                    Authentication authentication) {
        productService.update(id, request, authentication.getName());
        return ResponseEntity.ok("updated");
    }

    /**
     * 상품을 삭제합니다. (판매자 권한 전용)
     *
     * @param id             삭제할 상품 ID
     * @param authentication 현재 로그인된 유저 인증 정보
     * @return 성공 메시지
     */
    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    Authentication authentication) {
        productService.delete(id, authentication.getName());
        return ResponseEntity.ok("deleted");
    }

    /**
     * 상품을 찜 목록에 추가하거나 제거(토글)합니다. (인증 필요)
     *
     * @param id             찜할 상품 ID
     * @param authentication 현재 로그인된 유저 인증 정보
     * @return 찜 등록 여부 (wished: true/false)
     */
    @PostMapping("/wish/{id}")
    public ResponseEntity<?> toggleWish(@PathVariable Long id, Authentication authentication) {
        boolean isWished = productService.toggleWishlist(id, authentication.getName());
        return ResponseEntity.ok(Map.of("wished", isWished));
    }

    /**
     * 로그인된 사용자의 찜 목록을 조회합니다. (인증 필요)
     *
     * @param authentication 현재 로그인된 유저 인증 정보
     * @return 찜한 상품 목록
     */
    @GetMapping("/wishlist")
    public ResponseEntity<?> getWishlist(Authentication authentication) {
        return ResponseEntity.ok(productService.getWishlist(authentication.getName()));
    }

    /**
     * 외부 쇼핑몰 URL을 전달받아 상품명, 대표 이미지, 설명을 크롤링하여 반환합니다. (판매자 권한 전용)
     *
     * @param url 크롤링할 대상 외부 URL
     * @return 크롤링하여 추출된 상품 정보 Map
     */
    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/crawl")
    public ResponseEntity<?> crawlProductUrl(@RequestParam String url) {
        try {
            Map<String, String> data = productService.crawlProductUrl(url);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("외부 URL 크롤링 중 에러 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "정보를 불러오는 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
