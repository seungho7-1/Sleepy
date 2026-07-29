package com.sleepyproject.sleepy_backend.domain.product;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 슬라임 마켓에 등록된 상품 정보를 나타내는 도메인 엔티티 클래스입니다.
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 상품 고유 식별 번호 (PK, 데이터베이스에서 자동 증가)
    private Long id;

    // 상품명 (슬라임 이름 등)
    private String name;

    // 가격 (원화 단위)
    private int price;

    // 상품 이미지 저장 주소 (웹 URL 형식)
    @Column(length = 2000)
    private String imageUrl;

    // 상품 설명 (슬라임 느낌, 재질, 옵션 등 설명글)
    @Column(length = 2000)
    private String description;

    // 해당 상품을 판매하는 스토어/업체 이름 (예: "슬라임쿡", "와이영슬라임")
    private String shopName;

    // 실제 상품을 구매할 수 있는 외부 마켓/네이버스토어 등의 웹 결제 URL 링크
    @Column(length = 2000)
    private String purchaseUrl;

    // 슬라임 부가 정보

    private String texture; // 질감
    private String scent; // 향
    private String color; // 색상
    
    private LocalDate releaseDate; // 출시일

    // 상품 카테고리 (SLIME, SLANGY, MALLANGI, SQUISHY)
    private String category;

    // 상품 등록 시간
    private LocalDateTime createdAt;

    // 리뷰 수 (리뷰 작성/삭제 시 동기화)
    @Builder.Default
    @Column(columnDefinition = "int default 0")
    private int reviewCount = 0;

    // 평균 별점 (리뷰 작성/삭제 시 재계산)
    @Builder.Default
    @Column(columnDefinition = "double default 0")
    private double avgRating = 0.0;

    // 비디오 파일 URL 또는 외부 영상 링크
    @Column(length = 2000)
    private String videoUrl;
    
    // 비디오 종류 ('FILE', 'LINK', 'NONE')
    private String videoType;

    // 상품 상세 설명 이미지 주소 (선택)
    @Column(length = 2000)
    private String descriptionImageUrl;

    // 이 상품을 등록한 판매자 정보 (Member 엔티티와 N:1 다대일 지연 로딩 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Member seller;

    // 태그 양방향 연관관계 (N+1 문제 해결을 위한 지연/즉시 로딩용)
    @OneToMany(mappedBy = "product")
    private List<ProductTag> productTags = new ArrayList<>();

    @Builder
    public Product(Member seller, String name, int price, String description, String imageUrl, String shopName, String purchaseUrl, 
                   String texture, String scent, String color, LocalDate releaseDate, LocalDateTime createdAt,
                   String videoUrl, String videoType, String descriptionImageUrl, String category) {
        this.seller = seller;
        this.name = name;
        this.price = price;
        this.description = description;
        this.imageUrl = imageUrl;
        this.shopName = shopName;
        this.purchaseUrl = purchaseUrl;

        this.texture = texture;
        this.scent = scent;
        this.color = color;
        this.releaseDate = releaseDate;
        this.createdAt = createdAt;
        this.videoUrl = videoUrl;
        this.videoType = videoType;
        this.descriptionImageUrl = descriptionImageUrl;
        this.category = category;
    }

    /**
     * 상품 정보를 수정합니다. (더티 체킹 반영)
     */
    public void update(String name, int price, String description, String imageUrl, String shopName, String purchaseUrl, 
                       String texture, String scent, String color, LocalDate releaseDate,
                       String videoUrl, String videoType, String descriptionImageUrl, String category) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.imageUrl = imageUrl;
        this.shopName = shopName;
        this.purchaseUrl = purchaseUrl;

        this.texture = texture;
        this.scent = scent;
        this.color = color;
        this.releaseDate = releaseDate;
        this.videoUrl = videoUrl;
        this.videoType = videoType;
        this.descriptionImageUrl = descriptionImageUrl;
        this.category = category;
    }

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isHidden = false;

    public void hide() {
        this.isHidden = true;
    }

    public void unhide() {
        this.isHidden = false;
    }

    /**
     * 리뷰 수와 평균 별점을 업데이트합니다.
     * 리뷰 작성/삭제 시 호출합니다.
     */
    public void updateReviewStats(int reviewCount, double avgRating) {
        this.reviewCount = reviewCount;
        this.avgRating = avgRating;
    }

    /**
     * 쉼표(,)로 구분된 이미지 경로 중 첫 번째 이미지(대표 이미지)를 가져옵니다.
     *
     * @return 대표 이미지 URL 문자열 (이미지가 없을 시 빈 문자열 반환)
     */
    public String getFirstImageUrl() {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "";
        }
        return imageUrl.split(",")[0];
    }

    /**
     * 쉼표(,)로 연결되어 저장된 상품 추가 이미지 URL 전체를 리스트로 분할하여 반환합니다.
     *
     * @return 이미지 URL 리스트
     */
    public java.util.List<String> getImageUrlList() {
        if (imageUrl == null || imageUrl.isBlank()) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.asList(imageUrl.split(","));
    }

    /**
     * 쉼표(,)로 연결되어 저장된 상품 상세 페이지 설명용 이미지 URL 전체를 리스트로 분할하여 반환합니다.
     *
     * @return 상세 설명 이미지 URL 리스트
     */
    public java.util.List<String> getDescriptionImageUrlList() {
        if (descriptionImageUrl == null || descriptionImageUrl.isBlank()) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.asList(descriptionImageUrl.split(","));
    }
}