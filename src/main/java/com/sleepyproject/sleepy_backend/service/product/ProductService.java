package com.sleepyproject.sleepy_backend.service.product;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import com.sleepyproject.sleepy_backend.api.product.dto.ProductCreateRequest;
import com.sleepyproject.sleepy_backend.api.product.dto.ProductResponse;
import com.sleepyproject.sleepy_backend.api.product.dto.ProductUpdateRequest;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.product.Product;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.product.ProductRepository;
import com.sleepyproject.sleepy_backend.repository.product.TagRepository;
import com.sleepyproject.sleepy_backend.repository.product.ProductTagRepository;
import com.sleepyproject.sleepy_backend.domain.product.Tag;
import com.sleepyproject.sleepy_backend.domain.product.ProductTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final com.sleepyproject.sleepy_backend.repository.product.WishlistRepository wishlistRepository;
    private final TagRepository tagRepository;
    private final ProductTagRepository productTagRepository;
    private final com.sleepyproject.sleepy_backend.repository.review.ReviewRepository reviewRepository;
    private final com.sleepyproject.sleepy_backend.service.notification.NotificationService notificationService;

    /**
     * 전체 상품 목록 조회 로직
     *
     * @return 등록된 모든 상품 엔티티 리스트 (최신순 정렬)
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    /**
     * 상품 등록 처리 비즈니스 로직
     *
     * @param request 상품 등록용 DTO (상품명, 설명, 가격, 이미지 URL, 스토어명, 구매 링크 포함)
     * @param email   등록자의 이메일 (인증정보에서 추출)
     * @return 등록 완료된 상품의 고유 ID(PK) 반환
     */
    public Long create(ProductCreateRequest request, String username) {
        // 1. 등록하려는 회원(판매자)이 데이터베이스에 존재하는지 검증
        Member seller = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));

        String imagesString = request.getImageUrls() != null ? String.join(",", request.getImageUrls()) : "";
        String descImagesString = request.getDescriptionImageUrls() != null ? String.join(",", request.getDescriptionImageUrls()) : "";

        // 2. 전달받은 데이터를 바탕으로 Product 엔티티를 빌드(새 필드 반영)
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(imagesString) // 이미지 저장 주소 추가
                .shopName(seller.getShopName())   // 슬라임 마켓 스토어명 추가 (판매자 정보에서 가져옴)
                .purchaseUrl(request.getPurchaseUrl()) // 외부 구매 주소 링크 추가
                .texture(request.getTexture())
                .scent(request.getScent())
                .color(request.getColor())
                .releaseDate(request.getReleaseDate())
                .createdAt(LocalDateTime.now()) // 현재 시간 기록
                .seller(seller)
                .videoUrl(request.getVideoUrl())
                .videoType(request.getVideoType())
                .descriptionImageUrl(descImagesString)
                .category(request.getCategory())
                .build();

        productRepository.save(product);

        if (request.getTags() != null) {
            for (String tagName : request.getTags()) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(new Tag(tagName)));
                productTagRepository.save(new ProductTag(product, tag));
            }
        }

        // [찜 알림] 이 판매자를 찜한 유저들에게 신상품 등록 알림 발송
        // 중복 알림 방지를 위해 회원 ID 기준으로 distinct 처리
        wishlistRepository.findByProductSellerId(seller.getId()).stream()
                .map(w -> w.getMember())
                .filter(m -> !m.getId().equals(seller.getId()))
                .distinct()
                .forEach(wishlistMember -> notificationService.createNotificationByMember(
                        wishlistMember,
                        com.sleepyproject.sleepy_backend.domain.notification.NotificationType.WISHLIST_UPDATE,
                        seller.getShopName() + " 스토어에 새 슬라임이 등록되었습니다! - " + product.getName(),
                        "/product/" + product.getId()
                ));

        return product.getId();
    }

    /**
     * 상품 정보 수정 비즈니스 로직
     *
     * @param productId 수정할 대상 상품 ID
     * @param request   상품 수정용 DTO (수정된 데이터 값들이 들었음)
     * @param email     수정을 요청한 유저 이메일 (본인 소유 상품인지 체크용)
     */
    @Transactional
    @CacheEvict(value = "productDetail", key = "#productId")
    public void update(Long productId, ProductUpdateRequest request, String username) {
        // 1. 수정을 요청한 회원 정보 조회
        Member seller = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 2. 수정 대상 상품 정보 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 3. 소유권 체크: 본인이 등록한 상품인지 검증 (SELLER 역할이라 하더라도 본인 글만 수정 가능)
        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("본인이 등록한 상품만 수정할 수 있습니다.");
        }

        String imagesString = request.getImageUrls() != null ? String.join(",", request.getImageUrls()) : "";
        String descImagesString = request.getDescriptionImageUrls() != null ? String.join(",", request.getDescriptionImageUrls()) : "";

        // 4. 엔티티 수정 메서드 호출 (더티 체킹에 의해 트랜잭션 종료 시 반영됨)
        product.update(
                request.getName(),
                request.getPrice(),
                request.getDescription(),
                imagesString,  // 이미지 경로 추가
                seller.getShopName(),  // 마켓 스토어명 추가 (판매자 정보에서 가져옴)
                request.getPurchaseUrl(), // 외부 구매 링크 추가
                request.getTexture(),
                request.getScent(),
                request.getColor(),
                request.getReleaseDate(),
                request.getVideoUrl(),
                request.getVideoType(),
                descImagesString,
                request.getCategory()
        );
        
        productTagRepository.deleteByProduct(product);
        if (request.getTags() != null) {
            for (String tagName : request.getTags()) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(new Tag(tagName)));
                productTagRepository.save(new ProductTag(product, tag));
            }
        }
    }

    /**
     * 상품 삭제 비즈니스 로직
     *
     * @param productId 삭제할 상품 ID
     * @param email     삭제를 요청한 유저 이메일 (본인 소유 체크용)
     */
    @Transactional
    public void delete(Long productId, String username) {
        // 1. 유저 및 상품 정보 조회
        Member seller = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 2. 소유권 검증 (등록한 본인만 삭제 가능)
        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("본인이 등록한 상품만 삭제할 수 있습니다.");
        }

        // 3. 연관 데이터 선제 삭제 (외래키 제약조건 위배 방지)
        productTagRepository.deleteByProduct(product);
        wishlistRepository.deleteByProduct(product);
        reviewRepository.deleteByProductId(productId);

        // 4. DB에서 삭제
        productRepository.delete(product);
    }

    /**
     * 페이징 처리된 상품 목록 조회 로직 (검색 키워드 포함)
     *
     * @param keyword  검색 키워드 (상품명 검색용, 없거나 빈 문자열일 시 전체 조회)
     * @param pageable 페이징 및 정렬 조건 (Spring의 Pageable 객체)
     * @return DTO인 ProductResponse로 변환된 Page 객체 반환
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(String category, String keyword, Long sellerId, Pageable pageable) {
        Page<Product> products;

        log.info("카테고리: {}, 키워드: {}, 판매자: {} 로 검색 진행", category, keyword, sellerId);
        products = productRepository.findByCategoryAndKeywordAndSellerId(category, keyword, sellerId, pageable);

        // DB에서 가져온 엔티티 Page 객체를 응답용 DTO(ProductResponse) Page 객체로 매핑/변환
        return products.map(p -> {
            List<String> tags = p.getProductTags().stream()
                    .map(pt -> pt.getTag().getName())
                    .collect(Collectors.toList());
            return new ProductResponse(
                    p.getId(),
                    p.getName(),
                    p.getPrice(),
                    p.getDescription(),
                    p.getFirstImageUrl(),          // 첫번째 대표 이미지 노출 (하위호환용)
                    p.getShopName(),          // 스토어명 추가
                    p.getPurchaseUrl(),       // 결제 주소 링크 추가
                    p.getSeller().getId(),    // 등록한 판매자 ID (프론트 소유권 판별용)
                    p.getTexture(),
                    p.getScent(),
                    p.getColor(),
                    p.getReleaseDate(),
                    tags,
                    p.getVideoUrl(),
                    p.getVideoType(),
                    p.getImageUrlList(),
                    p.getDescriptionImageUrlList(),
                    p.getCategory(),
                    p.getReviewCount(),
                    p.getAvgRating(),
                    p.getSeller().getProfileImageUrl()
            );
        });
    }

    /**
     * 상품 단건 상세 조회 로직
     *
     * @param productId 상세 조회할 상품의 고유 ID
     * @return 조회된 상품 정보 DTO(ProductResponse) 반환
     */
    @Cacheable(value = "productDetail", key = "#productId")
    public ProductResponse getProductDetail(Long productId) {
        // 1. 상품 조회 (존재하지 않으면 예외 발생)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));

        List<String> tags = product.getProductTags().stream()
                .map(pt -> pt.getTag().getName())
                .collect(Collectors.toList());

        // 2. 응답 DTO 형태로 변환하여 반환
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getDescription(),
                product.getFirstImageUrl(),          // 첫번째 대표 이미지 노출 (하위호환용)
                product.getShopName(),          // 마켓 스토어명 추가
                product.getPurchaseUrl(),        // 외부 결제 링크 추가
                product.getSeller().getId(),      // 등록한 판매자 ID (프론트 소유권 판별용)
                product.getTexture(),
                product.getScent(),
                product.getColor(),
                product.getReleaseDate(),
                tags,
                product.getVideoUrl(),
                product.getVideoType(),
                product.getImageUrlList(),
                product.getDescriptionImageUrlList(),
                product.getCategory(),
                product.getReviewCount(),
                product.getAvgRating(),
                product.getSeller().getProfileImageUrl()
        );
    }

    /**
     * 위시리스트 토글(추가/삭제) 비즈니스 로직
     *
     * @param productId 위시리스트에 추가/삭제할 대상 상품 ID
     * @param email     요청한 유저 이메일
     * @return 위시리스트 추가 시 true, 삭제 시 false 반환
     */
    @Transactional
    public boolean toggleWishlist(Long productId, String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        var optionalWishlist = wishlistRepository.findByMemberAndProduct(member, product);
        if (optionalWishlist.isPresent()) {
            wishlistRepository.delete(optionalWishlist.get());
            return false;
        } else {
            wishlistRepository.save(com.sleepyproject.sleepy_backend.domain.product.Wishlist.builder()
                    .member(member)
                    .product(product)
                    .createdAt(LocalDateTime.now())
                    .build());
            return true;
        }
    }

    /**
     * 내 위시리스트 조회 로직
     *
     * @param email 요청한 유저 이메일
     * @return 유저가 찜한 상품 목록 (ProductResponse 리스트)
     */
    public List<ProductResponse> getWishlist(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        List<com.sleepyproject.sleepy_backend.domain.product.Wishlist> list = wishlistRepository.findByMember(member);
        return list.stream().map(w -> {
            Product p = w.getProduct();
            List<String> tags = p.getProductTags().stream()
                    .map(pt -> pt.getTag().getName())
                    .collect(Collectors.toList());
            return new ProductResponse(
                    p.getId(), p.getName(), p.getPrice(), p.getDescription(), p.getFirstImageUrl(),
                    p.getShopName(), p.getPurchaseUrl(), p.getSeller().getId(),
                    p.getTexture(), p.getScent(), p.getColor(), p.getReleaseDate(), tags,
                    p.getVideoUrl(), p.getVideoType(), p.getImageUrlList(), p.getDescriptionImageUrlList(),
                    p.getCategory(), p.getReviewCount(), p.getAvgRating(),
                    p.getSeller().getProfileImageUrl()
            );
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 외부 슬라임 상품 쇼핑몰 URL을 크롤링하여 상품 정보를 추출합니다.
     * - OpenGraph 메타 태그를 기반으로 상품명, 대표 이미지, 상세 설명을 파싱합니다.
     *
     * @param url 크롤링할 외부 상품 판매 링크
     * @return 파싱된 상품 정보 (name, imageUrl, description) Map 반환
     * @throws IOException Jsoup 연결 실패 또는 읽기 오류 발생 시
     */
    public Map<String, String> crawlProductUrl(String url) throws IOException {
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

        return data;
    }
}