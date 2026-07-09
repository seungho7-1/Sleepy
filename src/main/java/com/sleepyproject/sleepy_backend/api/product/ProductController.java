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
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>("parameters", headers);
            
            // Encode the url properly
            String apiUrl = "https://api.dub.co/metatags?url=" + java.net.URLEncoder.encode(url, "UTF-8");
            
            org.springframework.http.ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    apiUrl, 
                    org.springframework.http.HttpMethod.GET, 
                    entity, 
                    Map.class);
            
            Map<String, Object> response = responseEntity.getBody();
            
            if (response == null) {
                throw new RuntimeException("Failed to fetch metadata");
            }
            
            Map<String, String> data = new HashMap<>();
            data.put("name", (String) response.getOrDefault("title", ""));
            data.put("imageUrl", (String) response.getOrDefault("image", ""));
            data.put("description", (String) response.getOrDefault("description", ""));

            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "정보를 불러오는 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
