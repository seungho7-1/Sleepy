package com.sleepyproject.sleepy_backend.api.product.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// 상품 등록 요청을 전달받는 DTO 클래스
public class ProductCreateRequest {
    // 상품명
    private String name;
    
    // 상품 가격
    private int price;
    
    // 상품 설명
    private String description;
    
    // 상품 이미지 URL 링크 주소 목록 (최대 5개)
    private List<String> imageUrls;

    // 슬라임 판매 마켓 스토어명
    private String shopName;

    // 실제 결제 및 구매가 진행되는 외부 마켓 링크 (네이버스토어 등)
    private String purchaseUrl;

    // 슬라임 부가 정보

    private String texture; // 질감
    private String scent; // 향
    private String color; // 색상
    
    private LocalDate releaseDate; // 출시일
    
    private List<String> tags; // 태그

    private String category; // 카테고리 (SLIME, SLANGY, MALLANGI, SQUISHY)

    private String videoUrl; // 비디오 주소
    private String videoType; // 비디오 타입 (FILE, LINK, NONE)
    private List<String> descriptionImageUrls; // 상세 설명 이미지 URL 목록
}