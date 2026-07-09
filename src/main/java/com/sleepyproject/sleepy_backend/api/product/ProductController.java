package com.sleepyproject.sleepy_backend.api.product;

import com.sleepyproject.sleepy_backend.api.product.dto.ProductCreateRequest;
import com.sleepyproject.sleepy_backend.api.product.dto.ProductUpdateRequest;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.service.product.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.HashMap;
import java.util.Map;

@RequestMapping("/api/products")
@RequiredArgsConstructor
@RestController
public class ProductController {

    private ProductCreateRequest request;
    private Authentication authentication;
    private final ProductService productService;
    private final MemberRepository memberRepository;


    //상품 등록
    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody ProductCreateRequest request,
                                    Authentication authentication) {


        Long id = productService.create(request, authentication.getName());

        return ResponseEntity.ok(Map.of("productId", id));
    }

    //상품 목록 조회
    @GetMapping("/list")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        return ResponseEntity.ok(productService.getProducts(keyword, pageable));
    }

    //상품 상세 목록
    @GetMapping("/detail/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductDetail(id));

    }

    // 상품 수정
    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody ProductUpdateRequest request,
                                    Authentication authentication) {

        productService.update(id, request, authentication.getName());

        return ResponseEntity.ok("updated");
    }

    // 상품 삭제
    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    Authentication authentication) {

        productService.delete(id, authentication.getName());

        return ResponseEntity.ok("deleted");
    }

    // 찜하기 토글
    @PostMapping("/wish/{id}")
    public ResponseEntity<?> toggleWish(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        boolean isWished = productService.toggleWishlist(id, authentication.getName());
        return ResponseEntity.ok(Map.of("wished", isWished));
    }

    // 내 찜 목록
    @GetMapping("/wishlist")
    public ResponseEntity<?> getWishlist(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(productService.getWishlist(authentication.getName()));
    }

    // 상품 URL 자동 크롤링 API
    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/crawl")
    public ResponseEntity<?> crawlProductUrl(@RequestParam String url) {
        try {
            // Jsoup을 사용해 대상 사이트에 접속하여 HTML 문서를 읽어옵니다.
            org.jsoup.Connection connection = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(6000)
                    .referrer("https://www.google.com");
            
            Document doc = connection.get();
            
            // 1. 상품명 (OpenGraph og:title 우선, 없으면 페이지 타이틀)
            String name = "";
            Element ogTitle = doc.selectFirst("meta[property=og:title]");
            if (ogTitle != null) {
                name = ogTitle.attr("content");
            } else {
                name = doc.title();
            }

            // 2. 대표 이미지 (OpenGraph og:image)
            String imageUrl = "";
            Element ogImage = doc.selectFirst("meta[property=og:image]");
            if (ogImage != null) {
                imageUrl = ogImage.attr("content");
            }

            // 3. 설명 (OpenGraph og:description 우선, 없으면 일반 meta description)
            String description = "";
            Element ogDesc = doc.selectFirst("meta[property=og:description]");
            if (ogDesc != null) {
                description = ogDesc.attr("content");
            } else {
                Element metaDesc = doc.selectFirst("meta[name=description]");
                if (metaDesc != null) {
                    description = metaDesc.attr("content");
                }
            }

            Map<String, String> data = new HashMap<>();
            data.put("name", name);
            data.put("imageUrl", imageUrl);
            data.put("description", description);

            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "정보를 불러오는 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
